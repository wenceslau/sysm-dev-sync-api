package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.request.QuestionStatusUpdate;
import com.sysm.devsync.infrastructure.repositories.ProjectJpaRepository;
import com.sysm.devsync.infrastructure.repositories.QuestionJpaRepository;
import com.sysm.devsync.infrastructure.repositories.TagJpaRepository;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import com.sysm.devsync.infrastructure.repositories.WorkspaceJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.QuestionJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class QuestionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private QuestionJpaRepository questionJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private TagJpaRepository tagJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    // This ID is hardcoded in QuestionController, so we need a user with this ID for creation tests
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    private UserJpaEntity testAuthor;
    private ProjectJpaEntity testProject1;
    private ProjectJpaEntity testProject2;
    private TagJpaEntity tagJava;
    private TagJpaEntity tagSpring;

    @BeforeEach
    void setUp() {
        // Clear all relevant repositories before each test
        questionJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        tagJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll(); // Clear users last as others might depend on them

        // Ensure the fake authenticated user exists for tests that need it
        testAuthor = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Fake Author", "fake.author@example.com", UserRole.MEMBER)));
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

        // Create a test workspace for the projects
        WorkspaceJpaEntity testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor.getId())));

        // Create test projects for the questions
        testProject1 = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project 1", "Desc", testWorkspace.getId())));
        testProject2 = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project 2", "Desc", testWorkspace.getId())));

        // Create test tags
        tagJava = tagJpaRepository.saveAndFlush(TagJpaEntity.fromModel(Tag.create("Java", "#F89820")));
        tagSpring = tagJpaRepository.saveAndFlush(TagJpaEntity.fromModel(Tag.create("Spring", "#6DB33F")));
    }

    @Test
    @DisplayName("POST /questions - should create a new question successfully")
    void createQuestion_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new QuestionCreateUpdate("How to use Spring Boot?", "I need help with dependency injection.", testProject1.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdQuestion = questionJpaRepository.findAll().get(0);
        assertThat(createdQuestion.getTitle()).isEqualTo("How to use Spring Boot?");
        assertThat(createdQuestion.getProject().getId()).isEqualTo(testProject1.getId());
        assertThat(createdQuestion.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
        assertThat(createdQuestion.getStatus()).isEqualTo(QuestionStatus.OPEN);
    }

    @Test
    @DisplayName("GET /questions/{id} - should retrieve an existing question")
    void getQuestionById_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("Find Me", "Desc", testProject1.getId(), testAuthor.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));

        // Act & Assert
        mockMvc.perform(get("/questions/{id}", savedQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedQuestion.getId())))
                .andExpect(jsonPath("$.title", equalTo("Find Me")))
                .andExpect(jsonPath("$.projectId", equalTo(testProject1.getId())))
                .andExpect(jsonPath("$.authorId", equalTo(testAuthor.getId())));
    }

    @Test
    @DisplayName("GET /questions - should return paginated list of questions")
    void searchQuestions_withoutFilters_shouldReturnPaginatedAndSortedResults() throws Exception {
        // Arrange
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question C", "...", testProject1.getId(), testAuthor.getId())));
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question A", "...", testProject1.getId(), testAuthor.getId())));
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question B", "...", testProject1.getId(), testAuthor.getId())));
        questionJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/questions")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "title")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Question A"))
                .andExpect(jsonPath("$.items[1].title").value("Question B"));
    }

    @Test
    @DisplayName("GET /questions - should return questions filtered by query parameters")
    void searchQuestions_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        var q1 = com.sysm.devsync.domain.models.Question.create("Java Question", "...", testProject1.getId(), testAuthor.getId());
        q1.addTag(tagJava.getId());
        questionJpaRepository.save(QuestionJpaEntity.fromModel(q1));

        var q2 = com.sysm.devsync.domain.models.Question.create("Spring Question", "...", testProject1.getId(), testAuthor.getId());
        q2.changeStatus(QuestionStatus.RESOLVED);
        q2.addTag(tagSpring.getId());
        questionJpaRepository.save(QuestionJpaEntity.fromModel(q2));

        var q3 = com.sysm.devsync.domain.models.Question.create("Another Java Question", "...", testProject2.getId(), testAuthor.getId());
        q3.addTag(tagJava.getId());
        questionJpaRepository.save(QuestionJpaEntity.fromModel(q3));

        questionJpaRepository.flush();

        // Act & Assert - Filter by status
        mockMvc.perform(get("/questions").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Spring Question"));

        // Act & Assert - Filter by projectId
        mockMvc.perform(get("/questions").param("projectId", testProject2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Another Java Question"));

        // Act & Assert - Filter by tagsName
        mockMvc.perform(get("/questions").param("tagsName", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].title", containsInAnyOrder("Java Question", "Another Java Question")));

        // Act & Assert - Filter by multiple fields (status and projectId)
        mockMvc.perform(get("/questions")
                        .param("status", "OPEN")
                        .param("projectId", testProject1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Java Question"));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/questions")
                        .param("status", "RESOLVED")
                        .param("projectId", testProject2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("PUT /questions/{id} - should update an existing question's title and description")
    void updateQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("Old Title", "Old Desc", testProject1.getId(), testAuthor.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));

        var requestDto = new QuestionCreateUpdate("New Title", "New Description", testProject1.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/questions/{id}", savedQuestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getTitle()).isEqualTo("New Title");
        assertThat(updatedQuestion.get().getDescription()).isEqualTo("New Description");
    }

    @Test
    @DisplayName("PATCH /questions/{id}/status - should update a question's status")
    void updateQuestionStatus_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("Status Test", "Desc", testProject1.getId(), testAuthor.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));
        assertThat(savedQuestion.getStatus()).isEqualTo(QuestionStatus.OPEN); // Initial status

        var requestDto = new QuestionStatusUpdate(QuestionStatus.CLOSED);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(patch("/questions/{id}/status", savedQuestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getStatus()).isEqualTo(QuestionStatus.CLOSED);
    }

    @Test
    @DisplayName("DELETE /questions/{id} - should delete an existing question")
    void deleteQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("To Be Deleted", "Desc", testProject1.getId(), testAuthor.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));
        assertThat(questionJpaRepository.existsById(savedQuestion.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/questions/{id}", savedQuestion.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(questionJpaRepository.existsById(savedQuestion.getId())).isFalse();
    }

    @Test
    @DisplayName("POST /questions/{id}/tags/{tagId} - should add a tag to a question")
    void addTagToQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("Tag Me", "Desc", testProject1.getId(), testAuthor.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));
        assertThat(savedQuestion.getTags()).isEmpty();

        // Act & Assert
        mockMvc.perform(post("/questions/{id}/tags/{tagId}", savedQuestion.getId(), tagJava.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getTags()).hasSize(1);
        assertThat(updatedQuestion.get().getTags().iterator().next().getId()).isEqualTo(tagJava.getId());
    }

    @Test
    @DisplayName("DELETE /questions/{id}/tags/{tagId} - should remove a tag from a question")
    void removeTagFromQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = com.sysm.devsync.domain.models.Question.create("Untag Me", "Desc", testProject1.getId(), testAuthor.getId());
        question.addTag(tagJava.getId());
        var savedQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(question));
        assertThat(savedQuestion.getTags()).hasSize(1);

        // Act & Assert
        mockMvc.perform(delete("/questions/{id}/tags/{tagId}", savedQuestion.getId(), tagJava.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getTags()).isEmpty();
    }
}
