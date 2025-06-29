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

    private UserJpaEntity testAuthor1;
    private UserJpaEntity testAuthor2;
    private QuestionJpaEntity testQuestion1;
    private QuestionJpaEntity testQuestion2;

    @BeforeEach
    void setUp() {
        // Clean all relevant repositories to ensure test isolation
        answerJpaRepository.deleteAll();
        questionJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create users
        testAuthor1 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author One", "author1@test.com", UserRole.MEMBER)));
        testAuthor2 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author Two", "author2@test.com", UserRole.MEMBER)));

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

        // 3. Create a workspace and project for the questions
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor1.getId())));
        var testProject = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));

        // 4. Create questions to be answered
        var questionModel1 = Question.create("How to test?", "Details on testing.", testProject.getId(), testAuthor1.getId());
        testQuestion1 = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel1));

        var questionModel2 = Question.create("How to deploy?", "Details on deploying.", testProject.getId(), testAuthor2.getId());
        testQuestion2 = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel2));
    }

    @Test
    @DisplayName("POST /answers/questions/{qId} - should create a new answer successfully")
    void createAnswer_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new AnswerCreateUpdate("This is the correct way to test.");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/answers/questions/{questionId}", testQuestion1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdAnswer = answerJpaRepository.findAll().get(0);
        assertThat(createdAnswer.getContent()).isEqualTo("This is the correct way to test.");
        assertThat(createdAnswer.getQuestion().getId()).isEqualTo(testQuestion1.getId());
        assertThat(createdAnswer.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
        assertThat(createdAnswer.isAccepted()).isFalse();
    }

    @Test
    @DisplayName("GET /answers/{id} - should retrieve an existing answer")
    void getAnswerById_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("My Answer", testQuestion1.getId(), testAuthor1.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));

        // Act & Assert
        mockMvc.perform(get("/answers/{answerId}", savedAnswer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(savedAnswer.getId())))
                .andExpect(jsonPath("$.content", equalTo("My Answer")))
                .andExpect(jsonPath("$.questionId", equalTo(testQuestion1.getId())));
    }

    @Test
    @DisplayName("GET /answers - should return paginated and filtered answers")
    void searchAnswers_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        var answer1 = com.sysm.devsync.domain.models.Answer.create("First answer, not accepted", testQuestion1.getId(), testAuthor1.getId());
        answerJpaRepository.save(AnswerJpaEntity.fromModel(answer1));

        var answer2 = com.sysm.devsync.domain.models.Answer.create("Second answer, accepted", testQuestion1.getId(), testAuthor2.getId());
        answer2.accept();
        answerJpaRepository.save(AnswerJpaEntity.fromModel(answer2));

        var answer3 = com.sysm.devsync.domain.models.Answer.create("Third answer, for question 2", testQuestion2.getId(), testAuthor1.getId());
        answerJpaRepository.save(AnswerJpaEntity.fromModel(answer3));
        answerJpaRepository.flush();

        // Act & Assert - Filter by isAccepted=true
        mockMvc.perform(get("/answers").param("isAccepted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(answer2.getId()));

        // Act & Assert - Filter by authorId
        mockMvc.perform(get("/answers").param("authorId", testAuthor2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(answer2.getId()));

        // Act & Assert - Filter by questionId
        mockMvc.perform(get("/answers").param("questionId", testQuestion2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(answer3.getId()));

        // Act & Assert - Filter by multiple fields (isAccepted=false and questionId=testQuestion1)
        mockMvc.perform(get("/answers")
                        .param("isAccepted", "false")
                        .param("questionId", testQuestion1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(answer1.getId()));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/answers")
                        .param("isAccepted", "true")
                        .param("questionId", testQuestion2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    @DisplayName("PUT /answers/{id} - should update an existing answer")
    void updateAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("Old Content", testQuestion1.getId(), testAuthor1.getId());
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
        var answerModel = com.sysm.devsync.domain.models.Answer.create("A great answer", testQuestion1.getId(), testAuthor1.getId());
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
    @DisplayName("DELETE /answers/{id} - should delete an existing answer")
    void deleteAnswer_shouldSucceed() throws Exception {
        // Arrange
        var answerModel = com.sysm.devsync.domain.models.Answer.create("To be deleted", testQuestion1.getId(), testAuthor1.getId());
        var savedAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));
        assertThat(answerJpaRepository.existsById(savedAnswer.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/answers/{answerId}", savedAnswer.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(answerJpaRepository.existsById(savedAnswer.getId())).isFalse();
    }

    @Test
    @DisplayName("GET /answers/questions/{qId} - should return paginated answers")
    void getAnswersByQuestionId_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer C", testQuestion1.getId(), testAuthor1.getId())));
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer A", testQuestion1.getId(), testAuthor1.getId())));
        answerJpaRepository.save(AnswerJpaEntity.fromModel(com.sysm.devsync.domain.models.Answer.create("Answer B", testQuestion1.getId(), testAuthor1.getId())));
        answerJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/answers/questions/{questionId}", testQuestion1.getId())
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
