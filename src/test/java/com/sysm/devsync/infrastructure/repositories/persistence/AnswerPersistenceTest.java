package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
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
        workspaceOwnerJpa.setCreatedAt(Instant.now());
        workspaceOwnerJpa.setUpdatedAt(Instant.now());
        entityPersist(workspaceOwnerJpa);

        // Create Author User for Answers
        User authorDomain = User.create("Answer Author", "answer.author@example.com", UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain);
        entityPersist(authorUserJpa);

        // Create Workspace (requires owner)
        Workspace wsDomain = Workspace.create("Workspace for Answer Tests", "Workspace description", false, workspaceOwnerJpa.getId());
        WorkspaceJpaEntity workspaceJpa = new WorkspaceJpaEntity();
        workspaceJpa.setId(wsDomain.getId());
        workspaceJpa.setName(wsDomain.getName());
        workspaceJpa.setDescription(wsDomain.getDescription());
        workspaceJpa.setPrivate(wsDomain.isPrivate());
        workspaceJpa.setOwner(workspaceOwnerJpa); // Set the managed owner
        workspaceJpa.setCreatedAt(Instant.now());
        workspaceJpa.setUpdatedAt(Instant.now());
        entityPersist(workspaceJpa);

        // Create Project (requires workspace)
        Project projectDomain = Project.create("Project for Answer Tests", "Project description", workspaceJpa.getId());
        ProjectJpaEntity projectJpa = new ProjectJpaEntity();
        projectJpa.setId(projectDomain.getId());
        projectJpa.setName(projectDomain.getName());
        projectJpa.setDescription(projectDomain.getDescription());
        projectJpa.setWorkspace(workspaceJpa); // Set the managed workspace
        projectJpa.setCreatedAt(Instant.now());
        projectJpa.setUpdatedAt(Instant.now());
        entityPersist(projectJpa);

        // Create Questions (requires author and project)
        Question question1Domain = Question.create("Question 1 for Answers", "Description for Q1",  projectJpa.getId(), authorUserJpa.getId());
        question1Jpa = new QuestionJpaEntity();
        question1Jpa.setId(question1Domain.getId());
        question1Jpa.setTitle(question1Domain.getTitle());
        question1Jpa.setDescription(question1Domain.getDescription());
        question1Jpa.setStatus(question1Domain.getStatus());
        question1Jpa.setAuthor(authorUserJpa); // Set managed author
        question1Jpa.setProject(projectJpa); // Set managed project
        question1Jpa.setCreatedAt(Instant.now());
        question1Jpa.setUpdatedAt(Instant.now());
        entityPersist(question1Jpa);

        Question question2Domain = Question.create("Question 2 for Answers", "Description for Q2", projectJpa.getId(), authorUserJpa.getId());
        QuestionJpaEntity question2Jpa = new QuestionJpaEntity();
        question2Jpa.setId(question2Domain.getId());
        question2Jpa.setTitle(question2Domain.getTitle());
        question2Jpa.setDescription(question2Domain.getDescription());
        question2Jpa.setStatus(question2Domain.getStatus());
        question2Jpa.setAuthor(authorUserJpa);
        question2Jpa.setProject(projectJpa);
        question2Jpa.setCreatedAt(Instant.now());
        question2Jpa.setUpdatedAt(Instant.now());
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
            assertThat(foundInDb.isAccepted()).isEqualTo(answer1Domain.isAccepted());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(answer1Domain.getAuthorId());
            assertThat(foundInDb.getQuestion().getId()).isEqualTo(answer1Domain.getQuestionId());
            assertThat(foundInDb.getCreatedAt()).isNotNull();
            assertThat(foundInDb.getUpdatedAt()).isNotNull();

            // Verify retrieval via persistence layer
            Optional<Answer> foundAnswer = answerPersistence.findById(answer1Domain.getId());
            assertThat(foundAnswer).isPresent();
            assertThat(foundAnswer.get().getContent()).isEqualTo(answer1Domain.getContent());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when creating with null model")
        void create_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer model cannot be null");
        }

        @Test
        @DisplayName("should fail to create answer with non-existent Author ID due to FK constraint")
        void create_nonExistentAuthorId_shouldFail() {
            Answer answerWithInvalidAuthor = Answer.create(
                    "Invalid Author Answer", question1Jpa.getId(), UUID.randomUUID().toString()
            );
            assertThatThrownBy(() -> {
                create(answerWithInvalidAuthor);
            }).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("should fail to create answer with non-existent Question ID due to FK constraint")
        void create_nonExistentQuestionId_shouldFail() {
            Answer answerWithInvalidQuestion = Answer.create(
                    "Invalid Question Answer",  UUID.randomUUID().toString(), authorUserJpa.getId()
            );
            assertThatThrownBy(() -> {
                create(answerWithInvalidQuestion);
            }).isInstanceOf(ConstraintViolationException.class);
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
                    Instant.now() // New updatedAt (will be overwritten by @PreUpdate)
            );

            // Act
            assertDoesNotThrow(() -> update(updatedDomainAnswer));

            // Assert
            Optional<Answer> foundAnswerOpt = answerPersistence.findById(answer1Domain.getId());
            assertThat(foundAnswerOpt).isPresent();
            Answer foundAnswer = foundAnswerOpt.get();

            assertThat(foundAnswer.getContent()).isEqualTo("Updated: This is the first answer for question 1.");
            assertThat(foundAnswer.isAccepted()).isTrue();
            assertThat(foundAnswer.getCreatedAt().truncatedTo(ChronoUnit.MILLIS))
                    .isEqualTo(answer1Domain.getCreatedAt().truncatedTo(ChronoUnit.MILLIS)); // createdAt should not change
            assertThat(foundAnswer.getUpdatedAt()).isAfterOrEqualTo(foundAnswer.getCreatedAt()); // updatedAt should be updated
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when updating with null model")
        void update_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer model cannot be null");
        }

        @Test
        @DisplayName("update should effectively insert if ID does not exist (current behavior)")
        void update_nonExistentId_shouldInsert() {
            // Current `update` method behavior: if ID doesn't exist, `repository.save()` will insert.
            var newAnswerToUpdate = Answer.create(
                    "New Answer via Update", question1Jpa.getId(), authorUserJpa.getId()
            );
            // Ensure the ID is one that does not exist
            var answerToUpdate = Answer.build(
                    UUID.randomUUID().toString(), // Use a new, non-existent ID
                    newAnswerToUpdate.getQuestionId(),
                    newAnswerToUpdate.getAuthorId(),
                    newAnswerToUpdate.getCreatedAt(),
                    newAnswerToUpdate.getContent(),
                    newAnswerToUpdate.isAccepted(),
                    newAnswerToUpdate.getUpdatedAt()
            );

            assertDoesNotThrow(() -> update(answerToUpdate));

            Optional<Answer> foundAnswer = answerPersistence.findById(answerToUpdate.getId());
            assertThat(foundAnswer).isPresent();
            assertThat(foundAnswer.get().getContent()).isEqualTo("New Answer via Update");
        }

        @Test
        @DisplayName("should fail to update answer with non-existent Author ID due to FK constraint")
        void update_nonExistentAuthorId_shouldFail() {
            // Arrange: Create an initial valid answer
            create(answer1Domain);

            // Build an updated model with a non-existent author ID
            Answer updatedAnswer = Answer.build(
                    answer1Domain.getId(),
                    answer1Domain.getQuestionId(),
                    UUID.randomUUID().toString(), // Non-existent Author ID
                    answer1Domain.getCreatedAt(),
                    answer1Domain.getContent(),
                    answer1Domain.isAccepted(),
                    Instant.now()
            );

            // Act & Assert
            assertThatThrownBy(() -> {
                update(updatedAnswer);
            }).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("should fail to update answer with non-existent Question ID due to FK constraint")
        void update_nonExistentQuestionId_shouldFail() {
            // Arrange: Create an initial valid answer
            create(answer1Domain);

            // Build an updated model with a non-existent question ID
            Answer updatedAnswer = Answer.build(
                    answer1Domain.getId(),
                    UUID.randomUUID().toString(), // Non-existent Question ID
                    answer1Domain.getAuthorId(),
                    answer1Domain.getCreatedAt(),
                    answer1Domain.getContent(),
                    answer1Domain.isAccepted(),
                    Instant.now()
            );

            // Act & Assert
            assertThatThrownBy(() -> {
                update(updatedAnswer);
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete an answer by its ID")
        void deleteById_shouldRemoveAnswer() {
            // Arrange
            create(answer1Domain);
            assertThat(answerPersistence.existsById(answer1Domain.getId())).isTrue();

            // Act
            deleteById(answer1Domain.getId());

            // Assert
            assertThat(answerPersistence.existsById(answer1Domain.getId())).isFalse();
            assertThat(answerPersistence.findById(answer1Domain.getId())).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when deleting with null or blank ID")
        void deleteById_nullOrBlankId_shouldThrowException() {
            assertThatThrownBy(() -> deleteById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");

            assertThatThrownBy(() -> deleteById(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");
        }

        @Test
        @DisplayName("deleteById should not throw error for non-existent ID")
        void deleteById_nonExistentId_shouldNotThrowError() {
            assertDoesNotThrow(() -> deleteById(UUID.randomUUID().toString()));
        }
    }

    @Nested
    @DisplayName("findById Method Tests")
    class FindByIdTests {
        @Test
        @DisplayName("should return answer when found")
        void findById_whenAnswerExists_shouldReturnAnswer() {
            // Arrange
            create(answer1Domain);

            // Act
            Optional<Answer> foundAnswer = answerPersistence.findById(answer1Domain.getId());

            // Assert
            assertThat(foundAnswer).isPresent();
            assertThat(foundAnswer.get().getId()).isEqualTo(answer1Domain.getId());
            assertThat(foundAnswer.get().getContent()).isEqualTo(answer1Domain.getContent());
        }

        @Test
        @DisplayName("should return empty optional when answer not found")
        void findById_whenAnswerDoesNotExist_shouldReturnEmpty() {
            // Act
            Optional<Answer> foundAnswer = answerPersistence.findById(UUID.randomUUID().toString());

            // Assert
            assertThat(foundAnswer).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when finding with null or blank ID")
        void findById_nullOrBlankId_shouldThrowException() {
            assertThatThrownBy(() -> answerPersistence.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");

            assertThatThrownBy(() -> answerPersistence.findById(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("existsById Method Tests")
    class ExistsByIdTests {
        @Test
        @DisplayName("should return true when answer exists")
        void existsById_whenAnswerExists_shouldReturnTrue() {
            // Arrange
            create(answer1Domain);

            // Act
            boolean exists = answerPersistence.existsById(answer1Domain.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when answer does not exist")
        void existsById_whenAnswerDoesNotExist_shouldReturnFalse() {
            // Act
            boolean exists = answerPersistence.existsById(UUID.randomUUID().toString());

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when checking existence with null or blank ID")
        void existsById_nullOrBlankId_shouldThrowException() {
            assertThatThrownBy(() -> answerPersistence.existsById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");

            assertThatThrownBy(() -> answerPersistence.existsById(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Answer ID cannot be null or blank");
        }
    }

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

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent());
            assertThat(result.total()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return an empty page if no answers for question ID")
        void findAllByQuestionId_noMatches_shouldReturnEmptyPage() {
            Page domainPage = Page.of(0, 10);
            Pagination<Answer> result = answerPersistence.findAllByQuestionId(domainPage, UUID.randomUUID().toString()); // Non-existent question ID

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when question ID is null or empty")
        void findAllByQuestionId_nullOrEmptyId_shouldThrowException() {
            Page domainPage = Page.of(0, 10);
            assertThatThrownBy(() -> answerPersistence.findAllByQuestionId(domainPage, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID must not be null or empty"); // Note: Your AnswerPersistence uses "Project ID" in message

            assertThatThrownBy(() -> answerPersistence.findAllByQuestionId(domainPage, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID must not be null or empty"); // Note: Your AnswerPersistence uses "Project ID" in message
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (AbstractPersistence)")
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
            SearchQuery query = new SearchQuery(Page.of(0, 10), "");
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by content (case-insensitive partial match)")
        void findAll_filterByContent_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "content=SECOND"); // Test case-insensitivity
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getContent()).isEqualTo(answer2Domain.getContent());
        }

        @Test
        @DisplayName("should filter by accepted status")
        void findAll_filterByAcceptedStatus_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "isAccepted=true");
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getContent()).isEqualTo(answer3Domain.getContent());

            SearchQuery queryFalse = new SearchQuery(Page.of(0, 10), "isAccepted=false");
            Pagination<Answer> resultFalse = answerPersistence.findAll(queryFalse);
            assertThat(resultFalse.items()).hasSize(2);
            assertThat(resultFalse.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent());
        }

        @Test
        @DisplayName("should throw BusinessException for invalid isAccepted value")
        void findAll_invalidIsAcceptedValue_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "isAccepted=invalid");
            assertThatThrownBy(() -> answerPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid value for isAccepted field: invalid. Expected 'true' or 'false'.");
        }

        @Test
        @DisplayName("should filter by authorId")
        void findAll_filterByAuthorId_shouldReturnMatching() {
            // Create another author and answer for differentiation
            UserJpaEntity anotherAuthor = new UserJpaEntity(UUID.randomUUID().toString());
            anotherAuthor.setName("Another Answer Author");
            anotherAuthor.setEmail("another.answer.author@example.com");
            anotherAuthor.setRole(UserRole.MEMBER);
            anotherAuthor.setCreatedAt(Instant.now());
            anotherAuthor.setUpdatedAt(Instant.now());
            entityPersist(anotherAuthor);

            Answer anotherAnswer = Answer.create("Answer by another author", question1Jpa.getId(), anotherAuthor.getId());
            create(anotherAnswer);

            SearchQuery query = new SearchQuery(Page.of(0, 10), "authorId=" + authorUserJpa.getId());
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(3); // answer1, answer2, answer3 are by authorUser
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent(), answer3Domain.getContent());
        }

        @Test
        @DisplayName("should filter by authorName (case-insensitive partial match)")
        void findAll_filterByAuthorName_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "authorName=ANSWER AUTHOR"); // Test case-insensitivity
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent(), answer3Domain.getContent());
        }

        @Test
        @DisplayName("should filter by questionId")
        void findAll_filterByQuestionId_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "questionId=" + question1Jpa.getId());
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer2Domain.getContent());
        }

        @Test
        @DisplayName("should filter by multiple terms (OR logic)")
        void findAll_multipleTerms_OR_Logic_shouldReturnMatching() {
            // Search for answers with content "first" OR accepted status "true"
            SearchQuery query = new SearchQuery(Page.of(0, 10), "content=first#isAccepted=true");
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).hasSize(2); // answer1 (content has "first"), answer3 (isAccepted is true)
            assertThat(result.items()).extracting(Answer::getContent)
                    .containsExactlyInAnyOrder(answer1Domain.getContent(), answer3Domain.getContent());
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "invalidField=testValue");
            assertThatThrownBy(() -> answerPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should handle terms with no matches")
        void findAll_termWithNoMatches_shouldReturnEmptyPage() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "content=NonExistentAnswer");
            Pagination<Answer> result = answerPersistence.findAll(query);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            // Sort by content to ensure predictable pagination results
            SearchQuery queryPage1 = new SearchQuery(Page.of(0, 2, "content", "asc"), "");

            Pagination<Answer> result1 = answerPersistence.findAll(queryPage1);

            assertThat(result1.items()).hasSize(2);
            assertThat(result1.currentPage()).isEqualTo(0);
            assertThat(result1.perPage()).isEqualTo(2);
            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).extracting(Answer::getContent)
                    .containsExactly("This is an answer for question 2.", "This is the first answer for question 1."); // Ordered by content asc

            SearchQuery queryPage2 = new SearchQuery(Page.of(1, 2, "content", "asc"), "");
            Pagination<Answer> result2 = answerPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items()).extracting(Answer::getContent)
                    .containsExactly("This is the second answer for question 1."); // The last one
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
