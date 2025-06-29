package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.*;
import com.sysm.devsync.infrastructure.controllers.dto.request.CommentCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.*;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CommentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CommentJpaRepository commentJpaRepository;
    @Autowired
    private NoteJpaRepository noteJpaRepository;
    @Autowired
    private QuestionJpaRepository questionJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    // This ID is hardcoded in the controller for the "authenticated" user
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    private UserJpaEntity testAuthor1;
    private UserJpaEntity testAuthor2;
    private QuestionJpaEntity questionTarget;
    private NoteJpaEntity noteTarget;

    @BeforeEach
    void setUp() {
        // Clean all relevant repositories to ensure test isolation
        commentJpaRepository.deleteAll();
        noteJpaRepository.deleteAll();
        questionJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create users
        testAuthor1 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author One", "author1@test.com", UserRole.MEMBER)));
        testAuthor2 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author Two", "author2@test.com", UserRole.MEMBER)));

        // 2. Ensure the fake authenticated user (who will create the comment) exists
        if (!userJpaRepository.existsById(FAKE_AUTHENTICATED_USER_ID)) {
            UserJpaEntity fakeAuthUser = new UserJpaEntity();
            fakeAuthUser.setId(FAKE_AUTHENTICATED_USER_ID);
            fakeAuthUser.setName("Controller User");
            fakeAuthUser.setEmail("controller.user@example.com");
            fakeAuthUser.setRole(UserRole.ADMIN);
            fakeAuthUser.setCreatedAt(Instant.now());
            fakeAuthUser.setUpdatedAt(Instant.now());
            userJpaRepository.saveAndFlush(fakeAuthUser);
        }

        // 3. Create a workspace and project for the targets
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor1.getId())));
        var testProject = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));

        // 4. Create target entities (a Question and a Note)
        var questionModel = Question.create("Question Target", "Details", testProject.getId(), testAuthor1.getId());
        questionTarget = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel));

        var noteModel = Note.create("Note Target", "Content", testProject.getId(), testAuthor2.getId());
        noteTarget = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));
    }

    @Test
    @DisplayName("POST /comments - should create a new comment successfully")
    void createComment_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new CommentCreateUpdate(TargetType.QUESTION, questionTarget.getId(), "This is a new comment.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdComment = commentJpaRepository.findAll().get(0);
        assertThat(createdComment.getContent()).isEqualTo("This is a new comment.");
        assertThat(createdComment.getTargetType()).isEqualTo(TargetType.QUESTION);
        assertThat(createdComment.getTargetId()).isEqualTo(questionTarget.getId());
        assertThat(createdComment.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
    }

    @Test
    @DisplayName("GET /comments - should return paginated and filtered comments")
    void searchComments_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        var comment1 = Comment.create(TargetType.QUESTION, questionTarget.getId(), testAuthor1.getId(), "First comment on question");
        commentJpaRepository.save(CommentJpaEntity.fromModel(comment1));

        var comment2 = Comment.create(TargetType.QUESTION, questionTarget.getId(), testAuthor2.getId(), "Second comment on question");
        commentJpaRepository.save(CommentJpaEntity.fromModel(comment2));

        var comment3 = Comment.create(TargetType.NOTE, noteTarget.getId(), testAuthor1.getId(), "A comment on the note");
        commentJpaRepository.save(CommentJpaEntity.fromModel(comment3));
        commentJpaRepository.flush();

        // Act & Assert - Filter by targetType
        mockMvc.perform(get("/comments").param("targetType", "NOTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(comment3.getId()));

        // Act & Assert - Filter by targetId
        mockMvc.perform(get("/comments").param("targetId", questionTarget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        // Act & Assert - Filter by authorId
        mockMvc.perform(get("/comments").param("authorId", testAuthor2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(comment2.getId()));

        // Act & Assert - Filter by multiple fields (targetType=QUESTION and authorId=testAuthor1)
        mockMvc.perform(get("/comments")
                        .param("targetType", "QUESTION")
                        .param("authorId", testAuthor1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(comment1.getId()));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/comments")
                        .param("targetType", "NOTE")
                        .param("authorId", testAuthor2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /comments/{id} - should delete an existing comment")
    void deleteComment_shouldSucceed() throws Exception {
        // Arrange
        var commentModel = Comment.create(TargetType.NOTE, noteTarget.getId(), testAuthor1.getId(), "To be deleted");
        var savedComment = commentJpaRepository.saveAndFlush(CommentJpaEntity.fromModel(commentModel));
        assertThat(commentJpaRepository.existsById(savedComment.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/comments/{id}", savedComment.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(commentJpaRepository.existsById(savedComment.getId())).isFalse();
    }
}
