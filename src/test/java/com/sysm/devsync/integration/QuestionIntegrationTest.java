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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
    private ProjectJpaEntity testProject;
    private TagJpaEntity testTag;
    private WorkspaceJpaEntity testWorkspace;

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

        // Create a test workspace for the project
        testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor.getId())));

        // Create a test project for the questions
        testProject = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));
        testProject.setWorkspace(testWorkspace); // Manually set the relationship for JPA
        projectJpaRepository.saveAndFlush(testProject);

        // Create a test tag
        testTag = tagJpaRepository.saveAndFlush(TagJpaEntity.fromModel(Tag.create("Test Tag", "A tag for questions")));
    }

    @Test
    @DisplayName("POST /questions - should create a new question successfully")
    void createQuestion_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new QuestionCreateUpdate("How to use Spring Boot?", "I need help with dependency injection.", testProject.getId());
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
        assertThat(createdQuestion.getProject().getId()).isEqualTo(testProject.getId());
        assertThat(createdQuestion.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
        assertThat(createdQuestion.getStatus()).isEqualTo(QuestionStatus.OPEN);
    }

    @Test
    @DisplayName("POST /questions - should fail with 400 for invalid data (blank title)")
    void createQuestion_withInvalidData_shouldFail() throws Exception {
        // Arrange: Title is blank
        var requestDto = new QuestionCreateUpdate("", "Description", testProject.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title[0]", equalTo("Question title must not be blank")));
    }

    @Test
    @DisplayName("POST /questions - should fail with 404 if project does not exist")
    void createQuestion_withNonExistentProject_shouldFail() throws Exception {
        // Arrange
        var nonExistentProjectId = "00000000-0000-0000-0000-000000000000";
        var requestDto = new QuestionCreateUpdate("Question for Bad Project", "Desc", nonExistentProjectId);
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Project not found")));
    }

    @Test
    @DisplayName("GET /questions/{id} - should retrieve an existing question")
    void getQuestionById_shouldSucceed() throws Exception {
        // Arrange
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Find Me", "Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        var savedQuestion = questionJpaRepository.saveAndFlush(question);

        // Act & Assert
        mockMvc.perform(get("/questions/{id}", savedQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedQuestion.getId())))
                .andExpect(jsonPath("$.title", equalTo("Find Me")))
                .andExpect(jsonPath("$.projectId", equalTo(testProject.getId())))
                .andExpect(jsonPath("$.authorId", equalTo(testAuthor.getId())));
    }

    @Test
    @DisplayName("GET /questions/{id} - should return 404 for non-existent question")
    void getQuestionById_whenNotFound_shouldFail() throws Exception {
        // Arrange
        var nonExistentId = UUID.randomUUID().toString();

        // Act & Assert
        mockMvc.perform(get("/questions/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Question not found")));
    }

    @Test
    @DisplayName("PUT /questions/{id} - should update an existing question's title and description")
    void updateQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Old Title", "Old Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        var savedQuestion = questionJpaRepository.saveAndFlush(question);

        var requestDto = new QuestionCreateUpdate("New Title", "New Description", testProject.getId());
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
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Status Test", "Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        var savedQuestion = questionJpaRepository.saveAndFlush(question);
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
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("To Be Deleted", "Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        var savedQuestion = questionJpaRepository.saveAndFlush(question);
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
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Tag Me", "Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        var savedQuestion = questionJpaRepository.saveAndFlush(question);
        assertThat(savedQuestion.getTags()).isEmpty();

        // Act & Assert
        mockMvc.perform(post("/questions/{id}/tags/{tagId}", savedQuestion.getId(), testTag.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getTags()).hasSize(1);
        assertThat(updatedQuestion.get().getTags().iterator().next().getId()).isEqualTo(testTag.getId());
    }

    @Test
    @DisplayName("DELETE /questions/{id}/tags/{tagId} - should remove a tag from a question")
    void removeTagFromQuestion_shouldSucceed() throws Exception {
        // Arrange
        var question = QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Untag Me", "Desc", testProject.getId(), testAuthor.getId()));
        question.setProject(testProject);
        question.setAuthor(testAuthor);
        question.getTags().add(testTag); // Add tag directly for setup
        var savedQuestion = questionJpaRepository.saveAndFlush(question);
        assertThat(savedQuestion.getTags()).hasSize(1);

        // Act & Assert
        mockMvc.perform(delete("/questions/{id}/tags/{tagId}", savedQuestion.getId(), testTag.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<QuestionJpaEntity> updatedQuestion = questionJpaRepository.findById(savedQuestion.getId());
        assertThat(updatedQuestion).isPresent();
        assertThat(updatedQuestion.get().getTags()).isEmpty();
    }

    @Test
    @DisplayName("GET /questions - should return paginated list of questions")
    void searchQuestions_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question C", "...", testProject.getId(), testAuthor.getId())));
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question A", "...", testProject.getId(), testAuthor.getId())));
        questionJpaRepository.save(QuestionJpaEntity.fromModel(com.sysm.devsync.domain.models.Question.create("Question B", "...", testProject.getId(), testAuthor.getId())));
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
}
