package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.infrastructure.AbstractRepositoryTest;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

public class CommentJpaRepositoryTest extends AbstractRepositoryTest {

    // Prerequisite entities
    private UserJpaEntity authorUser;
    private QuestionJpaEntity questionTarget;
    private NoteJpaEntity noteTarget;

    // Entities under test
    private CommentJpaEntity comment1OnQuestion;
    private CommentJpaEntity comment2OnQuestion;
    private CommentJpaEntity commentOnNote;

    @BeforeEach
    void setUp() {
        // Use the inherited clear method
        clearRepositories();

        // 1. Create a User (Author)
        authorUser = new UserJpaEntity(UUID.randomUUID().toString());
        authorUser.setName("Comment Author");
        authorUser.setEmail("comment.author@example.com");
        authorUser.setRole(UserRole.MEMBER);
        authorUser.setCreatedAt(Instant.now());
        authorUser.setUpdatedAt(Instant.now());
        entityManager.persist(authorUser);

        // 2. Create Target Entities (e.g., a Question and a Note)
        // We need a full hierarchy for these targets
        UserJpaEntity owner = new UserJpaEntity(UUID.randomUUID().toString());
        owner.setName("Owner");
        owner.setEmail("owner.comment@example.com");
        owner.setRole(UserRole.ADMIN);
        owner.setCreatedAt(Instant.now());
        owner.setUpdatedAt(Instant.now());
        entityManager.persist(owner);

        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity(UUID.randomUUID().toString());
        workspace.setName("Workspace for Comment Targets");
        workspace.setOwner(owner);
        workspace.setCreatedAt(Instant.now());
        workspace.setUpdatedAt(Instant.now());
        entityManager.persist(workspace);

        ProjectJpaEntity project = new ProjectJpaEntity(UUID.randomUUID().toString());
        project.setName("Project for Comment Targets");
        project.setWorkspace(workspace);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        entityManager.persist(project);

        // Create a Question as a target
        questionTarget = new QuestionJpaEntity(UUID.randomUUID().toString());
        questionTarget.setTitle("Question to be commented on");
        questionTarget.setDescription("Desc");
        questionTarget.setStatus(QuestionStatus.OPEN);
        questionTarget.setAuthor(owner);
        questionTarget.setProject(project);
        questionTarget.setCreatedAt(Instant.now());
        questionTarget.setUpdatedAt(Instant.now());
        entityManager.persist(questionTarget);

        // Create a Note as another target
        noteTarget = new NoteJpaEntity(UUID.randomUUID().toString());
        noteTarget.setTitle("Note to be commented on");
        noteTarget.setContent("Content");
        noteTarget.setAuthor(owner);
        noteTarget.setProject(project);
        noteTarget.setVersion(0);
        noteTarget.setCreatedAt(Instant.now());
        noteTarget.setUpdatedAt(Instant.now());
        entityManager.persist(noteTarget);

        entityManager.flush();

        // 3. Create Comment Entities (in memory)
        comment1OnQuestion = new CommentJpaEntity(UUID.randomUUID().toString());
        comment1OnQuestion.setContent("This is the first comment on the question.");
        comment1OnQuestion.setTargetType(TargetType.QUESTION);
        comment1OnQuestion.setTargetId(questionTarget.getId());
        comment1OnQuestion.setAuthor(authorUser);
        comment1OnQuestion.setCreatedAt(Instant.now());
        comment1OnQuestion.setUpdatedAt(Instant.now());

        comment2OnQuestion = new CommentJpaEntity(UUID.randomUUID().toString());
        comment2OnQuestion.setContent("This is another comment on the same question.");
        comment2OnQuestion.setTargetType(TargetType.QUESTION);
        comment2OnQuestion.setTargetId(questionTarget.getId());
        comment2OnQuestion.setAuthor(authorUser);
        comment2OnQuestion.setCreatedAt(Instant.now());
        comment2OnQuestion.setUpdatedAt(Instant.now());

        commentOnNote = new CommentJpaEntity(UUID.randomUUID().toString());
        commentOnNote.setContent("This is a comment on the note.");
        commentOnNote.setTargetType(TargetType.NOTE);
        commentOnNote.setTargetId(noteTarget.getId());
        commentOnNote.setAuthor(authorUser);
        commentOnNote.setCreatedAt(Instant.now());
        commentOnNote.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Save and Find Tests")
    class SaveAndFindTests {
        @Test
        @DisplayName("should save a comment and find it by id")
        void saveAndFindById() {
            // Act
            CommentJpaEntity savedComment = commentJpaRepository.save(comment1OnQuestion);
            entityManager.flush();
            entityManager.clear();

            Optional<CommentJpaEntity> foundCommentOpt = commentJpaRepository.findById(savedComment.getId());

            // Assert
            assertThat(foundCommentOpt).isPresent();
            CommentJpaEntity foundComment = foundCommentOpt.get();
            assertThat(foundComment.getContent()).isEqualTo(comment1OnQuestion.getContent());
            assertThat(foundComment.getTargetType()).isEqualTo(TargetType.QUESTION);
            assertThat(foundComment.getTargetId()).isEqualTo(questionTarget.getId());
            assertThat(foundComment.getAuthor().getId()).isEqualTo(authorUser.getId());
            assertThat(foundComment.getCreatedAt()).isNotNull();
            assertThat(foundComment.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should fail to save comment with null content")
        void save_withNullContent_shouldFail() {
            // Arrange
            comment1OnQuestion.setContent(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                commentJpaRepository.save(comment1OnQuestion);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should fail to save comment with null author")
        void save_withNullAuthor_shouldFail() {
            // Arrange
            comment1OnQuestion.setAuthor(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                commentJpaRepository.save(comment1OnQuestion);
                entityManager.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Update and Delete Tests")
    class UpdateAndDeleteTests {
        @Test
        @DisplayName("should update an existing comment")
        void updateComment() {
            // Arrange
            CommentJpaEntity persistedComment = commentJpaRepository.save(comment1OnQuestion);
            entityManager.flush();
            Instant originalUpdatedAt = persistedComment.getUpdatedAt();

            sleep(100); // Ensure updatedAt will be different

            // Act
            persistedComment.setContent("This is the updated content.");
            persistedComment.setUpdatedAt(Instant.now()); // Update timestamp
            commentJpaRepository.save(persistedComment);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<CommentJpaEntity> updatedCommentOpt = commentJpaRepository.findById(persistedComment.getId());
            assertThat(updatedCommentOpt).isPresent();
            CommentJpaEntity updatedComment = updatedCommentOpt.get();
            assertThat(updatedComment.getContent()).isEqualTo("This is the updated content.");
            assertThat(updatedComment.getUpdatedAt()).isAfter(originalUpdatedAt);
        }

        @Test
        @DisplayName("should delete a comment by id")
        void deleteById() {
            // Arrange
            CommentJpaEntity persistedComment = commentJpaRepository.save(comment1OnQuestion);
            entityManager.flush();
            String idToDelete = persistedComment.getId();

            // Act
            commentJpaRepository.deleteById(idToDelete);
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<CommentJpaEntity> deletedComment = commentJpaRepository.findById(idToDelete);
            assertThat(deletedComment).isNotPresent();
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {
        @Test
        @DisplayName("findAllByTargetTypeAndTargetId should return all comments for a specific question")
        void findAllByTarget_forQuestion_shouldReturnMatchingComments() {
            // Arrange
            commentJpaRepository.save(comment1OnQuestion); // Target: question
            commentJpaRepository.save(comment2OnQuestion); // Target: question
            commentJpaRepository.save(commentOnNote);      // Target: note
            entityManager.flush();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<CommentJpaEntity> questionComments = commentJpaRepository.findAllByTargetTypeAndTargetId(
                    TargetType.QUESTION, questionTarget.getId(), pageable
            );

            // Assert
            assertThat(questionComments.getTotalElements()).isEqualTo(2);
            assertThat(questionComments.getContent()).extracting(CommentJpaEntity::getContent)
                    .containsExactlyInAnyOrder(comment1OnQuestion.getContent(), comment2OnQuestion.getContent());
        }

        @Test
        @DisplayName("findAllByTargetTypeAndTargetId should return all comments for a specific note")
        void findAllByTarget_forNote_shouldReturnMatchingComments() {
            // Arrange
            commentJpaRepository.save(comment1OnQuestion); // Target: question
            commentJpaRepository.save(comment2OnQuestion); // Target: question
            commentJpaRepository.save(commentOnNote);      // Target: note
            entityManager.flush();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<CommentJpaEntity> noteComments = commentJpaRepository.findAllByTargetTypeAndTargetId(
                    TargetType.NOTE, noteTarget.getId(), pageable
            );

            // Assert
            assertThat(noteComments.getTotalElements()).isEqualTo(1);
            assertThat(noteComments.getContent().get(0).getContent()).isEqualTo(commentOnNote.getContent());
        }

        @Test
        @DisplayName("findAllByTargetTypeAndTargetId should return an empty page for a target with no comments")
        void findAllByTarget_noMatches_shouldReturnEmptyPage() {
            // Arrange
            commentJpaRepository.save(commentOnNote); // Only save comment for the note
            entityManager.flush();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<CommentJpaEntity> questionComments = commentJpaRepository.findAllByTargetTypeAndTargetId(
                    TargetType.QUESTION, questionTarget.getId(), pageable
            );

            // Assert
            assertThat(questionComments.getTotalElements()).isZero();
            assertThat(questionComments.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Specification Tests")
    class SpecificationTests {
        @BeforeEach
        void setUpSpecs() {
            commentJpaRepository.save(comment1OnQuestion);
            commentJpaRepository.save(comment2OnQuestion);
            commentJpaRepository.save(commentOnNote);
            entityManager.flush();
        }

        @Test
        @DisplayName("should filter comments by content")
        void findAll_withSpecification_byContent() {
            // Arrange
            Specification<CommentJpaEntity> spec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("content")), "%another comment%");
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<CommentJpaEntity> result = commentJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(comment2OnQuestion.getId());
        }

        @Test
        @DisplayName("should filter comments by author ID")
        void findAll_withSpecification_byAuthorId() {
            // Arrange
            Specification<CommentJpaEntity> spec = (root, query, cb) ->
                    cb.equal(root.get("author").get("id"), authorUser.getId());
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<CommentJpaEntity> result = commentJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }
}
