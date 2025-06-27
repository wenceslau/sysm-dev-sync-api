package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.AnswerCreateUpdate;
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

public class AnswerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AnswerJpaRepository answerJpaRepository;
    @Autowired
    private QuestionJpaRepository questionJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    // This ID is hardcoded in the controller, so we need a user with this ID for creation tests
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    private UserJpaEntity testAuthor;
    private QuestionJpaEntity testQuestion;

    @BeforeEach
    void setUp() {
        // Clean all relevant repositories to ensure test isolation
        answerJpaRepository.deleteAll();
        questionJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create a user to be the author
        testAuthor = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Test Author", "author@test.com", UserRole.MEMBER)));

        // 2. Ensure the fake authenticated user (who will create the answer) exists
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

        // 3. Create a workspace and project for the question
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor.getId())));
        var testProject = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));

        // 4. Create a question to be answered
        var questionModel = Question.create("How to test?", "Details on testing.", testProject.getId(), testAuthor.getId());
        testQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel));
    }

    @Test
    @DisplayName("POST /questions/{qId} - should create a new answer successfully")
    void createAnswer_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new AnswerCreateUpdate("This is the correct way to test.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/answers/questions/{questionId}", testQuestion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdAnswer = answerJpaRepository.findAll().get(0);
        assertThat(createdAnswer.getContent()).isEqualTo("This is the correct way to test.");
        assertThat(createdAnswer.getQuestion().getId()).isEqualTo(testQuestion.getId());
        assertThat(createdAnswer.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
        assertThat(createdAnswer.isAccepted()).isFalse();
    }

    @Test
    @DisplayName("POST /questions/{qId} - should fail with 404 for non-existent question")
    void createAnswer_forNonExistentQuestion_shouldFail() throws Exception {
        // Arrange
        var nonExistentQuestionId = UUID.randomUUID().toString();
        var requestDto = new AnswerCreateUpdate("Some answer");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/answers/questions/{questionId}", nonExistentQuestionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Question not found")));
    }

    @Test
    @DisplayName("GET /answers/{id} - should retrieve an existing answer")
    void getAnswerById_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("My Answer", testQuestion.getId(), testAuthor.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));

        // Act & Assert
        mockMvc.perform(get("/answers/{answerId}", savedAnswer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedAnswer.getId())))
                .andExpect(jsonPath("$.content", equalTo("My Answer")))
                .andExpect(jsonPath("$.questionId", equalTo(testQuestion.getId())));
    }

    @Test
    @DisplayName("PUT /answers/{id} - should update an existing answer")
    void updateAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("Old Content", testQuestion.getId(), testAuthor.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));

        var requestDto = new AnswerCreateUpdate("New, updated content.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/answers/{answerId}", savedAnswer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<AnswerJpaEntity> updatedAnswer = answerJpaRepository.findById(savedAnswer.getId());
        assertThat(updatedAnswer).isPresent();
        assertThat(updatedAnswer.get().getContent()).isEqualTo("New, updated content.");
    }

    @Test
    @DisplayName("PATCH /answers/{id}/accept - should mark an answer as accepted")
    void acceptAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("A great answer", testQuestion.getId(), testAuthor.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));
        assertThat(savedAnswer.isAccepted()).isFalse();

        // Act & Assert
        mockMvc.perform(patch("/answers/{answerId}/accept", savedAnswer.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<AnswerJpaEntity> updatedAnswer = answerJpaRepository.findById(savedAnswer.getId());
        assertThat(updatedAnswer).isPresent();
        assertThat(updatedAnswer.get().isAccepted()).isTrue();
    }

    @Test
    @DisplayName("PATCH /answers/{id}/reject - should un-mark an answer as accepted")
    void rejectAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("An accepted answer", testQuestion.getId(), testAuthor.getId());
        answerModel.accept(); // Start as accepted
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));
        assertThat(savedAnswer.isAccepted()).isTrue();

        // Act & Assert
        mockMvc.perform(patch("/answers/{answerId}/reject", savedAnswer.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<AnswerJpaEntity> updatedAnswer = answerJpaRepository.findById(savedAnswer.getId());
        assertThat(updatedAnswer).isPresent();
        assertThat(updatedAnswer.get().isAccepted()).isFalse();
    }

    @Test
    @DisplayName("DELETE /answers/{id} - should delete an existing answer")
    void deleteAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("To be deleted", testQuestion.getId(), testAuthor.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));
        assertThat(answerJpaRepository.existsById(savedAnswer.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/answers/{answerId}", savedAnswer.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(answerJpaRepository.existsById(savedAnswer.getId())).isFalse();
    }

    @Test
    @DisplayName("GET /questions/{qId} - should return paginated answers")
    void getAnswersByQuestionId_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer C", testQuestion.getId(), testAuthor.getId())));
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer A", testQuestion.getId(), testAuthor.getId())));
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer B", testQuestion.getId(), testAuthor.getId())));
        answerJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/answers/questions/{questionId}", testQuestion.getId())
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "content")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].content").value("Answer A"))
                .andExpect(jsonPath("$.items[1].content").value("Answer B"));
    }
}
