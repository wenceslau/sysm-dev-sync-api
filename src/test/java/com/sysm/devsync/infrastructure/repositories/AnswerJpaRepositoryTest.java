package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnswerJpaRepositoryTest extends AbstractRepositoryTest {

    // Prerequisite entities
    private UserJpaEntity authorUser;
    private QuestionJpaEntity question1;

    // Entities under test
    private AnswerJpaEntity answer1;
    private AnswerJpaEntity answer2;
    private AnswerJpaEntity answer3;


    @BeforeEach
    void setUp() {
        // Use the inherited clear method
        clearRepositories();

        // 1. Create Users
        UserJpaEntity workspaceOwner = new UserJpaEntity(UUID.randomUUID().toString());
        workspaceOwner.setName("Workspace Owner");
        workspaceOwner.setEmail("ws.owner.answer@example.com");
        workspaceOwner.setRole(UserRole.ADMIN);
        workspaceOwner.setCreatedAt(Instant.now());
        workspaceOwner.setUpdatedAt(Instant.now());
        entityPersist(workspaceOwner);

        authorUser = new UserJpaEntity(UUID.randomUUID().toString());
        authorUser.setName("Answer Author");
        authorUser.setEmail("answer.author@example.com");
        authorUser.setRole(UserRole.MEMBER);
        authorUser.setCreatedAt(Instant.now());
        authorUser.setUpdatedAt(Instant.now());
        entityPersist(authorUser);

        // 2. Create Workspace
        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity(UUID.randomUUID().toString());
        workspace.setName("Workspace for Answer Tests");
        workspace.setOwner(workspaceOwner);
        workspace.setCreatedAt(Instant.now());
        workspace.setUpdatedAt(Instant.now());
        entityPersist(workspace);

        // 3. Create Project
        ProjectJpaEntity project = new ProjectJpaEntity(UUID.randomUUID().toString());
        project.setName("Project for Answer Tests");
        project.setWorkspace(workspace);
        project.setDescription("This project is used for testing answers.");
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        entityPersist(project);

        // 4. Create Questions
        question1 = new QuestionJpaEntity(UUID.randomUUID().toString());
        question1.setTitle("Question 1 for Answers");
        question1.setDescription("This is the first question.");
        question1.setStatus(QuestionStatus.OPEN);
        question1.setAuthor(authorUser);
        question1.setProject(project);
        question1.setCreatedAt(Instant.now());
        question1.setUpdatedAt(Instant.now());
        entityPersist(question1);

        QuestionJpaEntity question2 = new QuestionJpaEntity(UUID.randomUUID().toString());
        question2.setTitle("Question 2 for Answers");
        question2.setDescription("This is the second question.");
        question2.setStatus(QuestionStatus.OPEN);
        question2.setAuthor(authorUser);
        question2.setProject(project);
        question2.setCreatedAt(Instant.now());
        question2.setUpdatedAt(Instant.now());
        entityPersist(question2);

        entityManager.flush();

        // 5. Create Answer Entities (in memory, to be used in tests)
        answer1 = new AnswerJpaEntity(UUID.randomUUID().toString());
        answer1.setContent("This is the first answer for question 1.");
        answer1.setAccepted(false);
        answer1.setAuthor(authorUser);
        answer1.setQuestion(question1);
        answer1.setCreatedAt(Instant.now());
        answer1.setUpdatedAt(Instant.now());

        answer2 = new AnswerJpaEntity(UUID.randomUUID().toString());
        answer2.setContent("This is the second answer for question 1.");
        answer2.setAccepted(false);
        answer2.setAuthor(authorUser);
        answer2.setQuestion(question1);
        answer2.setCreatedAt(Instant.now());
        answer2.setUpdatedAt(Instant.now());

        answer3 = new AnswerJpaEntity(UUID.randomUUID().toString());
        answer3.setContent("This is an answer for question 2.");
        answer3.setAccepted(true);
        answer3.setAuthor(authorUser);
        answer3.setQuestion(question2);
        answer3.setCreatedAt(Instant.now());
        answer3.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Save and Find Tests")
    class SaveAndFindTests {
        @Test
        @DisplayName("should save an answer and find it by id")
        void saveAndFindById() {
            // Act
            AnswerJpaEntity savedAnswer = answerJpaRepository.save(answer1);
            entityManager.flush();
            entityManager.clear();

            Optional<AnswerJpaEntity> foundAnswerOpt = answerJpaRepository.findById(savedAnswer.getId());

            // Assert
            assertThat(foundAnswerOpt).isPresent();
            AnswerJpaEntity foundAnswer = foundAnswerOpt.get();
            assertThat(foundAnswer.getContent()).isEqualTo(answer1.getContent());
            assertThat(foundAnswer.isAccepted()).isFalse();
            assertThat(foundAnswer.getAuthor().getId()).isEqualTo(authorUser.getId());
            assertThat(foundAnswer.getQuestion().getId()).isEqualTo(question1.getId());
            assertThat(foundAnswer.getCreatedAt()).isNotNull();
            assertThat(foundAnswer.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should fail to save answer with null content")
        void save_withNullContent_shouldFail() {
            // Arrange
            answer1.setContent(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                answerJpaRepository.save(answer1);
                flushAndClear();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should fail to save answer with null author")
        void save_withNullAuthor_shouldFail() {
            // Arrange
            answer1.setAuthor(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                answerJpaRepository.save(answer1);
                flushAndClear();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should fail to save answer with null question")
        void save_withNullQuestion_shouldFail() {
            // Arrange
            answer1.setQuestion(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                answerJpaRepository.save(answer1);
                flushAndClear();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Update and Delete Tests")
    class UpdateAndDeleteTests {
        @Test
        @DisplayName("should update an existing answer")
        void updateAnswer() {
            // Arrange
            AnswerJpaEntity persistedAnswer = answerJpaRepository.save(answer1);
            flushAndClear();
            Instant originalUpdatedAt = persistedAnswer.getUpdatedAt();

            sleep(100); // Ensure the updatedAt timestamp changes

            // Act
            persistedAnswer.setContent("This is the updated content.");
            persistedAnswer.setAccepted(true);
            persistedAnswer.setUpdatedAt(Instant.now());
            answerJpaRepository.save(persistedAnswer);
            flushAndClear();
            entityManager.clear();

            // Assert
            Optional<AnswerJpaEntity> updatedAnswerOpt = answerJpaRepository.findById(persistedAnswer.getId());
            assertThat(updatedAnswerOpt).isPresent();
            AnswerJpaEntity updatedAnswer = updatedAnswerOpt.get();
            assertThat(updatedAnswer.getContent()).isEqualTo("This is the updated content.");
            assertThat(updatedAnswer.isAccepted()).isTrue();
            assertThat(updatedAnswer.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("should delete an answer by id")
        void deleteById() {
            // Arrange
            AnswerJpaEntity persistedAnswer = answerJpaRepository.save(answer1);
            flushAndClear();
            String idToDelete = persistedAnswer.getId();

            // Act
            answerJpaRepository.deleteById(idToDelete);
            flushAndClear();
            entityManager.clear();

            // Assert
            Optional<AnswerJpaEntity> deletedAnswer = answerJpaRepository.findById(idToDelete);
            assertThat(deletedAnswer).isNotPresent();
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {
        @Test
        @DisplayName("findAllByQuestion_Id should return all answers for a specific question")
        void findAllByQuestion_Id_shouldReturnMatchingAnswers() {
            // Arrange
            answerJpaRepository.save(answer1); // For question1
            answerJpaRepository.save(answer2); // For question1
            answerJpaRepository.save(answer3); // For question2
            flushAndClear();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<AnswerJpaEntity> question1Answers = answerJpaRepository.findAllByQuestion_Id(question1.getId(), pageable);

            // Assert
            assertThat(question1Answers.getTotalElements()).isEqualTo(2);
            assertThat(question1Answers.getContent()).extracting(AnswerJpaEntity::getContent)
                    .containsExactlyInAnyOrder(answer1.getContent(), answer2.getContent());
        }

        @Test
        @DisplayName("findAllByQuestion_Id should return an empty page for a question with no answers")
        void findAllByQuestion_Id_noMatches_shouldReturnEmptyPage() {
            // Arrange
            answerJpaRepository.save(answer3); // Only save answer for question2
            flushAndClear();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<AnswerJpaEntity> question1Answers = answerJpaRepository.findAllByQuestion_Id(question1.getId(), pageable);

            // Assert
            assertThat(question1Answers.getTotalElements()).isZero();
            assertThat(question1Answers.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Specification Tests")
    class SpecificationTests {
        @BeforeEach
        void setUpSpecs() {
            answerJpaRepository.save(answer1); // Not accepted
            answerJpaRepository.save(answer2); // Not accepted
            answerJpaRepository.save(answer3); // Accepted
            flushAndClear();
        }

        @Test
        @DisplayName("should filter answers by content")
        void findAll_withSpecification_byContent() {
            // Arrange
            Specification<AnswerJpaEntity> spec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("content")), "%second answer%");
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<AnswerJpaEntity> result = answerJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(answer2.getId());
        }

        @Test
        @DisplayName("should filter answers by accepted status")
        void findAll_withSpecification_byAcceptedStatus() {
            // Arrange
            Specification<AnswerJpaEntity> spec = (root, query, cb) ->
                    cb.isTrue(root.get("isAccepted"));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<AnswerJpaEntity> result = answerJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(answer3.getId());
        }

        @Test
        @DisplayName("should filter answers by author ID")
        void findAll_withSpecification_byAuthorId() {
            // Arrange
            Specification<AnswerJpaEntity> spec = (root, query, cb) ->
                    cb.equal(root.get("author").get("id"), authorUser.getId());
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<AnswerJpaEntity> result = answerJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }
}
