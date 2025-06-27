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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CommentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CommentJpaRepository commentJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private QuestionJpaRepository questionJpaRepository;
    @Autowired
    private AnswerJpaRepository answerJpaRepository;
    @Autowired
    private NoteJpaRepository noteJpaRepository;

    // This ID is hardcoded in the controller for the "authenticated" user
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    private UserJpaEntity testAuthor;
    private QuestionJpaEntity testQuestion;
    private AnswerJpaEntity testAnswer;
    private NoteJpaEntity testNote;

    @BeforeEach
    void setUp() {
        // Clean all repositories to ensure test isolation
        commentJpaRepository.deleteAll();
        answerJpaRepository.deleteAll();
        questionJpaRepository.deleteAll();
        noteJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create a user to be the author of parent objects
        testAuthor = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Test Author", "author@test.com", UserRole.MEMBER)));

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

        // 3. Create a workspace and project
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor.getId())));
        var testProject = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));

        // 4. Create target entities to comment on
        var questionModel = Question.create("How to test?", "Details on testing.", testProject.getId(), testAuthor.getId());
        testQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel));

        var answerModel = Answer.create("This is an answer.", testQuestion.getId(), testAuthor.getId());
        testAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));

        var noteModel = Note.create("My Test Note", "Content of the note.", testProject.getId(), testAuthor.getId());
        testNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));
    }

    @Test
    @DisplayName("POST /comments - should create a new comment on a Question")
    void createComment_onQuestion_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new CommentCreateUpdate(TargetType.QUESTION, testQuestion.getId(), "This is a comment on a question.");
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
        assertThat(createdComment.getContent()).isEqualTo("This is a comment on a question.");
        assertThat(createdComment.getTargetType()).isEqualTo(TargetType.QUESTION);
        assertThat(createdComment.getTargetId()).isEqualTo(testQuestion.getId());
        assertThat(createdComment.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
    }

    @Test
    @DisplayName("POST /comments - should fail for a non-existent target")
    void createComment_forNonExistentTarget_shouldFail() throws Exception {
        // Arrange
        var nonExistentId = UUID.randomUUID().toString();
        var requestDto = new CommentCreateUpdate(TargetType.NOTE, nonExistentId, "This comment should fail.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Note not found")));
    }

    @Test
    @DisplayName("PUT /comments/{id} - should update an existing comment")
    void updateComment_shouldSucceed() throws Exception {
        // Arrange
        var commentModel = Comment.create(TargetType.NOTE, testNote.getId(), testAuthor.getId(), "Old content");
        var savedComment = commentJpaRepository.saveAndFlush(CommentJpaEntity.fromModel(commentModel));

        var requestDto = new CommentCreateUpdate(TargetType.NOTE, testNote.getId(), "New, updated content.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/comments/{id}", savedComment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<CommentJpaEntity> updatedComment = commentJpaRepository.findById(savedComment.getId());
        assertThat(updatedComment).isPresent();
        assertThat(updatedComment.get().getContent()).isEqualTo("New, updated content.");
    }

    @Test
    @DisplayName("DELETE /comments/{id} - should delete an existing comment")
    void deleteComment_shouldSucceed() throws Exception {
        // Arrange
        var commentModel = Comment.create(TargetType.ANSWER, testAnswer.getId(), testAuthor.getId(), "To be deleted");
        var savedComment = commentJpaRepository.saveAndFlush(CommentJpaEntity.fromModel(commentModel));
        assertThat(commentJpaRepository.existsById(savedComment.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/comments/{id}", savedComment.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(commentJpaRepository.existsById(savedComment.getId())).isFalse();
    }

    @Test
    @DisplayName("GET /comments - should return comments filtered by target")
    void searchComments_byTarget_shouldReturnFilteredResults() throws Exception {
        // Arrange
        // Create comments on different targets
        commentJpaRepository.save(CommentJpaEntity.fromModel(Comment.create(TargetType.QUESTION, testQuestion.getId(), testAuthor.getId(), "Comment on Question")));
        commentJpaRepository.save(CommentJpaEntity.fromModel(Comment.create(TargetType.NOTE, testNote.getId(), testAuthor.getId(), "Comment 1 on Note")));
        commentJpaRepository.save(CommentJpaEntity.fromModel(Comment.create(TargetType.NOTE, testNote.getId(), testAuthor.getId(), "Comment 2 on Note")));
        commentJpaRepository.flush();

        // Act & Assert - Search for comments on the NOTE
        mockMvc.perform(get("/comments")
                        .param("terms", "targetType=NOTE#targetId=" + testNote.getId())
                        .param("sort", "content")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].content").value("Comment 1 on Note"))
                .andExpect(jsonPath("$.items[1].content").value("Comment 2 on Note"));
    }

    @Test
    @DisplayName("GET /comments - should return comments filtered by content")
    void searchComments_byContent_shouldReturnFilteredResults() throws Exception {
        // Arrange
        commentJpaRepository.save(CommentJpaEntity.fromModel(Comment.create(TargetType.QUESTION, testQuestion.getId(), testAuthor.getId(), "A specific comment")));
        commentJpaRepository.save(CommentJpaEntity.fromModel(Comment.create(TargetType.NOTE, testNote.getId(), testAuthor.getId(), "Another comment")));
        commentJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/comments")
                        .param("terms", "content=specific"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].content").value("A specific comment"));
    }
}
