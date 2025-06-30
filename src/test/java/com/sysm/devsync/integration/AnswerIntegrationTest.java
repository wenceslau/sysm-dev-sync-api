package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Answer;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.AnswerCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.*;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


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

    // Define constant, predictable UUIDs for test users
    private static final String QUESTION_OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ANSWER_OWNER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String ANOTHER_USER_ID = "33333333-3333-3333-3333-333333333333";
    private static final String ADMIN_ID = "44444444-4444-4444-4444-444444444444";

    private QuestionJpaEntity testQuestion;
    private AnswerJpaEntity testAnswer;

    @BeforeEach
    void setUp() {
        // Clean all relevant repositories to ensure test isolation
        answerJpaRepository.deleteAll();
        questionJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create users for various test scenarios using the constant IDs
        Instant now = Instant.now();
        var questionOwner = User.build(QUESTION_OWNER_ID, now, now, "Question Owner", "q.owner@test.com", null, null, UserRole.MEMBER);
        var answerOwner = User.build(ANSWER_OWNER_ID, now, now, "Answer Owner", "a.owner@test.com", null, null, UserRole.MEMBER);
        var anotherUser = User.build(ANOTHER_USER_ID, now, now, "Another User", "another@test.com", null, null, UserRole.MEMBER);
        var adminUser = User.build(ADMIN_ID, now, now, "Admin User", "admin@test.com", null, null, UserRole.ADMIN);

        userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(questionOwner));
        userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(answerOwner));
        userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(anotherUser));
        userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(adminUser));

        // 2. Create a workspace and project
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, questionOwner.getId())));
        var project = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project", "Desc", testWorkspace.getId())));

        // 3. Create a question owned by 'questionOwner'
        var questionModel = Question.create("How to test security?", "Details on testing.", project.getId(), questionOwner.getId());
        testQuestion = questionJpaRepository.saveAndFlush(QuestionJpaEntity.fromModel(questionModel));

        // 4. Create an answer owned by 'answerOwner'
        var answerModel = Answer.create("This is the answer.", testQuestion.getId(), answerOwner.getId());
        testAnswer = answerJpaRepository.saveAndFlush(AnswerJpaEntity.fromModel(answerModel));
    }

    @Nested
    @DisplayName("General Functionality Tests")
    class GeneralFunctionalityTests {

        @Test
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // Any authenticated member can create
        @DisplayName("POST /answers/questions/{qId} - should create a new answer successfully")
        void createAnswer_shouldSucceed() throws Exception {
            var requestDto = new AnswerCreateUpdate("This is a new answer from another user.");
            var requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(post("/answers/questions/{questionId}", testQuestion.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));

            // Verify DB state
            var createdAnswer = answerJpaRepository.findByAuthorId(ANOTHER_USER_ID).get(0);
            assertThat(createdAnswer.getContent()).isEqualTo("This is a new answer from another user.");
            assertThat(createdAnswer.getQuestion().getId()).isEqualTo(testQuestion.getId());
            assertThat(createdAnswer.getAuthor().getId()).isEqualTo(ANOTHER_USER_ID);
        }

        @Test
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // Any authenticated member can read
        @DisplayName("GET /answers/{id} - should retrieve an existing answer")
        void getAnswerById_shouldSucceed() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/answers/{answerId}", testAnswer.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", equalTo(testAnswer.getId())))
                    .andExpect(jsonPath("$.content", equalTo("This is the answer.")))
                    .andExpect(jsonPath("$.questionId", equalTo(testQuestion.getId())));
        }

        @Test
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // Any authenticated member can search
        @DisplayName("GET /answers - should return paginated and filtered answers")
        void searchAnswers_withFilters_shouldReturnFilteredResults() throws Exception {
            // Arrange
            var answer1 = Answer.create("First answer, not accepted", testQuestion.getId(), QUESTION_OWNER_ID);
            answerJpaRepository.save(AnswerJpaEntity.fromModel(answer1));

            var answer2 = Answer.create("Second answer, accepted", testQuestion.getId(), ANSWER_OWNER_ID);
            answer2.accept();
            answerJpaRepository.save(AnswerJpaEntity.fromModel(answer2));
            answerJpaRepository.flush();

            // Act & Assert - Filter by isAccepted=true
            mockMvc.perform(get("/answers").param("isAccepted", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.items[0].id").value(answer2.getId()));

            // Act & Assert - Filter by authorId
            mockMvc.perform(get("/answers").param("authorId", QUESTION_OWNER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.items[0].id").value(answer1.getId()));
        }

        @Test
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // Any authenticated member can read
        @DisplayName("GET /answers/questions/{qId} - should return paginated answers")
        void getAnswersByQuestionId_shouldReturnPaginatedResults() throws Exception {
            // Arrange
            answerJpaRepository.save(AnswerJpaEntity.fromModel(Answer.create("Answer C", testQuestion.getId(), QUESTION_OWNER_ID)));
            answerJpaRepository.save(AnswerJpaEntity.fromModel(Answer.create("Answer A", testQuestion.getId(), QUESTION_OWNER_ID)));
            // testAnswer is "Answer B"
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
                    .andExpect(jsonPath("$.items[1].content").value("Answer C"));
        }
    }

    @Nested
    @DisplayName("Update Answer Security Tests")
    class UpdateAnswerSecurity {

        @Test
        @DisplayName("PUT /answers/{id} - should succeed for the owner")
        @WithMockUser(username = ANSWER_OWNER_ID, roles = "MEMBER") // Authenticated as the answer owner
        void updateAnswer_asOwner_shouldSucceed() throws Exception {
            var requestDto = new AnswerCreateUpdate("Updated content by owner.");
            var requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/answers/{answerId}", testAnswer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNoContent());

            Optional<AnswerJpaEntity> updatedAnswer = answerJpaRepository.findById(testAnswer.getId());
            assertThat(updatedAnswer).isPresent();
            assertThat(updatedAnswer.get().getContent()).isEqualTo("Updated content by owner.");
        }

        @Test
        @DisplayName("PUT /answers/{id} - should succeed for an admin")
        @WithMockUser(username = ADMIN_ID, roles = "ADMIN") // Authenticated as an admin
        void updateAnswer_asAdmin_shouldSucceed() throws Exception {
            var requestDto = new AnswerCreateUpdate("Updated content by admin.");
            var requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/answers/{answerId}", testAnswer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("PUT /answers/{id} - should fail with 403 for a different user")
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // Authenticated as a non-owner, non-admin
        void updateAnswer_asDifferentUser_shouldFailWith403() throws Exception {
            var requestDto = new AnswerCreateUpdate("This update should fail.");
            var requestJson = objectMapper.writeValueAsString(requestDto);

            mockMvc.perform(put("/answers/{answerId}", testAnswer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete Answer Security Tests")
    class DeleteAnswerSecurity {
        @Test
        @DisplayName("DELETE /answers/{id} - should succeed for the owner")
        @WithMockUser(username = ANSWER_OWNER_ID, roles = "MEMBER")
        void deleteAnswer_asOwner_shouldSucceed() throws Exception {
            mockMvc.perform(delete("/answers/{answerId}", testAnswer.getId()))
                    .andExpect(status().isNoContent());

            assertThat(answerJpaRepository.existsById(testAnswer.getId())).isFalse();
        }

        @Test
        @DisplayName("DELETE /answers/{id} - should succeed for an admin")
        @WithMockUser(username = ADMIN_ID, roles = "ADMIN")
        void deleteAnswer_asAdmin_shouldSucceed() throws Exception {
            mockMvc.perform(delete("/answers/{answerId}", testAnswer.getId()))
                    .andExpect(status().isNoContent());

            assertThat(answerJpaRepository.existsById(testAnswer.getId())).isFalse();
        }

        @Test
        @DisplayName("DELETE /answers/{id} - should fail with 403 for a different user")
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER")
        void deleteAnswer_asDifferentUser_shouldFailWith403() throws Exception {
            mockMvc.perform(delete("/answers/{answerId}", testAnswer.getId()))
                    .andExpect(status().isForbidden());

            assertThat(answerJpaRepository.existsById(testAnswer.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("Accept/Reject Answer Security Tests")
    class AcceptAnswerSecurity {

        @Test
        @DisplayName("PATCH /answers/{id}/accept - should succeed for the question owner")
        @WithMockUser(username = QUESTION_OWNER_ID, roles = "MEMBER") // Authenticated as the question owner
        void acceptAnswer_asQuestionOwner_shouldSucceed() throws Exception {
            mockMvc.perform(patch("/answers/{answerId}/accept", testAnswer.getId()))
                    .andExpect(status().isNoContent());

            Optional<AnswerJpaEntity> updatedAnswer = answerJpaRepository.findById(testAnswer.getId());
            assertThat(updatedAnswer).isPresent();
            assertThat(updatedAnswer.get().isAccepted()).isTrue();
        }

        @Test
        @DisplayName("PATCH /answers/{id}/accept - should succeed for an admin")
        @WithMockUser(username = ADMIN_ID, roles = "ADMIN") // Authenticated as an admin
        void acceptAnswer_asAdmin_shouldSucceed() throws Exception {
            mockMvc.perform(patch("/answers/{answerId}/accept", testAnswer.getId()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("PATCH /answers/{id}/accept - should fail with 403 for the answer owner")
        @WithMockUser(username = ANSWER_OWNER_ID, roles = "MEMBER") // The answer owner cannot accept their own answer
        void acceptAnswer_asAnswerOwner_shouldFailWith403() throws Exception {
            mockMvc.perform(patch("/answers/{answerId}/accept", testAnswer.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATCH /answers/{id}/accept - should fail with 403 for a different user")
        @WithMockUser(username = ANOTHER_USER_ID, roles = "MEMBER") // A random user cannot accept
        void acceptAnswer_asDifferentUser_shouldFailWith403() throws Exception {
            mockMvc.perform(patch("/answers/{answerId}/accept", testAnswer.getId()))
                    .andExpect(status().isForbidden());
        }
    }
}
