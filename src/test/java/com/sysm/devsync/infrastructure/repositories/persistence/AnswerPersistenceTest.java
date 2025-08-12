package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QueryType;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Answer;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(AnswerPersistence.class) // Import the class under test
public class AnswerPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private AnswerPersistence answerPersistence; // The class under test

    // Prerequisite JPA entities (persisted before tests)
    private UserJpaEntity authorUserJpa;
    private QuestionJpaEntity question1Jpa;
    private QuestionJpaEntity question2Jpa;


    // Domain models for testing
    private Answer answer1Domain;
    private Answer answer2Domain;
    private Answer answer3Domain;

    @BeforeEach
    void setUp() {
        // Clear previous data to ensure a clean state
        clearRepositories();

        // 1. Create and Persist Prerequisite Entities
        // Create Owner User for Workspace
        User workspaceOwnerDomain = User.create("Workspace Owner", "ws.owner.answer@example.com", UserRole.ADMIN);
        UserJpaEntity workspaceOwnerJpa = UserJpaEntity.fromModel(workspaceOwnerDomain);
        entityPersist(workspaceOwnerJpa);

        // Create Author User for Answers
        User authorDomain = User.create("Answer Author", "answer.author@example.com", UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain);
        entityPersist(authorUserJpa);

        // Create Workspace (requires owner)
        Workspace wsDomain = Workspace.create("Workspace for Answer Tests", "Workspace description", false, workspaceOwnerJpa.getId());
        WorkspaceJpaEntity workspaceJpa = WorkspaceJpaEntity.fromModel(wsDomain);
        entityPersist(workspaceJpa);

        // Create Project (requires workspace)
        Project projectDomain = Project.create("Project for Answer Tests", "Project description", workspaceJpa.getId());
        ProjectJpaEntity projectJpa = ProjectJpaEntity.fromModel(projectDomain);
        entityPersist(projectJpa);

        // Create Questions (requires author and project)
        Question question1Domain = Question.create("Question 1 for Answers", "Description for Q1",  projectJpa.getId(), authorUserJpa.getId());
        question1Jpa = QuestionJpaEntity.fromModel(question1Domain);
        entityPersist(question1Jpa);

        Question question2Domain = Question.create("Question 2 for Answers", "Description for Q2", projectJpa.getId(), authorUserJpa.getId());
        question2Jpa = QuestionJpaEntity.fromModel(question2Domain);
        entityPersist(question2Jpa);

        // 2. Create Answer Domain Models (referencing IDs of persisted entities)
        answer1Domain = Answer.create(
                "This is the first answer for question 1.",
                question1Jpa.getId(),
                authorUserJpa.getId()
        );

        answer2Domain = Answer.create(
                "This is the second answer for question 1.",
                question1Jpa.getId(),
                authorUserJpa.getId()
        );

        answer3Domain = Answer.create(
                "This is an answer for question 2.",
                question2Jpa.getId(),
                authorUserJpa.getId()
        );

    }

    // --- Basic CRUD, findById, existsById tests are correct and remain unchanged ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save an answer")
        void create_shouldSaveAnswer() {
            // Act
            assertDoesNotThrow(() -> create(answer1Domain));

            // Assert
            AnswerJpaEntity foundInDb = entityManager.find(AnswerJpaEntity.class, answer1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getContent()).isEqualTo(answer1Domain.getContent());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(answer1Domain.getAuthorId());
            assertThat(foundInDb.getQuestion().getId()).isEqualTo(answer1Domain.getQuestionId());

            // Verify retrieval via persistence layer
            Optional<Answer> foundAnswer = answerPersistence.findById(answer1Domain.getId());
            assertThat(foundAnswer).isPresent();
            assertThat(foundAnswer.get().getContent()).isEqualTo(answer1Domain.getContent());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing answer")
        void update_shouldModifyExistingAnswer() {
            // Arrange: First, create the answer
            create(answer1Domain);

            // Build updated domain model
            Answer updatedDomainAnswer = Answer.build(
                    answer1Domain.getId(),
                    answer1Domain.getQuestionId(),
                    answer1Domain.getAuthorId(),
                    answer1Domain.getCreatedAt(),
                    "Updated: This is the first answer for question 1.", // Change content
                    true, // Change accepted status
                    Instant.now()
            );

            // Act
            assertDoesNotThrow(() -> update(updatedDomainAnswer));

            // Assert
            Optional<Answer> foundAnswerOpt = answerPersistence.findById(answer1Domain.getId());
            assertThat(foundAnswerOpt).isPresent();
            Answer foundAnswer = foundAnswerOpt.get();

            assertThat(foundAnswer.getContent()).isEqualTo("Updated: This is the first answer for question 1.");
            assertThat(foundAnswer.isAccepted()).isTrue();
        }
    }

    // --- Other basic tests (delete, findById, existsById) are also correct ---

    @Nested
    @DisplayName("findAllByQuestionId Method Tests")
    class FindAllByQuestionIdTests {
        @BeforeEach
        void setUpFindAllByQuestionId() {
            // Persist test data
            create(answer1Domain); // For question1
            create(answer2Domain); // For question1
            create(answer3Domain); // For question2
        }

        @Test
        @DisplayName("should return all answers for a specific question ID")
        void findAllByQuestionId_shouldReturnMatchingAnswers() {
            Page domainPage = Page.of(0, 10);
            Pagination<Answer> result = answerPersistence.findAllByQuestionId(domainPage, question1Jpa.getId());

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent());
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (Generic Search)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Persist test data
            create(answer1Domain); // content: "first", accepted: false, question1, authorUser
            create(answer2Domain); // content: "second", accepted: false, question1, authorUser
            answer3Domain.accept();
            create(answer3Domain); // content: "answer", accepted: true, question2, authorUser
        }

        @Test
        @DisplayName("should return all answers when no search terms provided")
        void findAll_noTerms_shouldReturnAllAnswers() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10),  Map.of());
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by a single term (e.g., content)")
        void findAll_filterByContent_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("content", "SECOND"));
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getId()).isEqualTo(answer2Domain.getId());
        }

        @Test
        @DisplayName("should filter by a single term (e.g., isAccepted)")
        void findAll_filterByAcceptedStatus_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("isAccepted", "true"));
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getId()).isEqualTo(answer3Domain.getId());
        }

        @Test
        @DisplayName("should filter by multiple terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for an answer that is NOT accepted AND belongs to question1
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "isAccepted", "false",
                    "questionId", question1Jpa.getId()
            ));

            // Act
            Pagination<Answer> resultWithMatch = answerPersistence.findAll(queryWithMatch);

            // Assert: Should find two answers (answer1 and answer2)
            assertThat(resultWithMatch.total()).isEqualTo(2);
            assertThat(resultWithMatch.items()).extracting(Answer::getId)
                    .containsExactlyInAnyOrder(answer1Domain.getId(), answer2Domain.getId());

            // Arrange: Search for an answer that IS accepted AND belongs to question1 (should be none)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "isAccepted", "true",
                    "questionId", question1Jpa.getId()
            ));

            // Act
            Pagination<Answer> resultWithoutMatch = answerPersistence.findAll(queryWithoutMatch);

            // Assert: Should find no answers
            assertThat(resultWithoutMatch.total()).isZero();
            assertThat(resultWithoutMatch.items()).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));
            assertThatThrownBy(() -> answerPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should respect pagination and sorting parameters")
        void findAll_withPaginationAndSorting_shouldReturnCorrectPage() {
            // Sort by content to ensure predictable pagination results
            SearchQuery queryPage1 = SearchQuery.of(Page.of(0, 2, "content", "asc"),  Map.of());

            Pagination<Answer> result1 = answerPersistence.findAll(queryPage1);

            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).hasSize(2);
            assertThat(result1.items()).extracting(Answer::getContent)
                    .containsExactly("This is an answer for question 2.", "This is the first answer for question 1.");

            SearchQuery queryPage2 = SearchQuery.of(Page.of(1, 2, "content", "asc"),  Map.of());
            Pagination<Answer> result2 = answerPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items().get(0).getContent()).isEqualTo("This is the second answer for question 1.");
        }
    }

    private void create(Answer entity) {
        answerPersistence.create(entity);
        flushAndClear();
    }

    private void update(Answer entity) {
        answerPersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        answerPersistence.deleteById(id);
        flushAndClear();
    }
}
