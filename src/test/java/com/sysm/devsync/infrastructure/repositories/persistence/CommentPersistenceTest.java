package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QueryType;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Comment;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.User;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(CommentPersistence.class) // Import the class under test
public class CommentPersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private CommentPersistence commentPersistence; // The class under test

    // Prerequisite JPA entities (persisted before tests)
    private UserJpaEntity authorUserJpa;
    private QuestionJpaEntity questionTargetJpa;
    private NoteJpaEntity noteTargetJpa;

    // Domain models for testing
    private Comment comment1OnQuestion;
    private Comment comment2OnQuestion;
    private Comment commentOnNote;

    @BeforeEach
    void setUp() {
        // Clear previous data to ensure a clean state
        clearRepositories();

        // 1. Create and Persist Prerequisite Entities
        User authorDomain = User.create("Comment Author", "comment.author@example.com",  UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain);
        entityPersist(authorUserJpa);

        // Create a full hierarchy for target entities
        User ownerDomain = User.create("Owner", "owner.comment@example.com", UserRole.ADMIN);
        UserJpaEntity ownerJpa = UserJpaEntity.fromModel(ownerDomain);
        entityPersist(ownerJpa);

        Workspace wsDomain = Workspace.create("Workspace for Comment Targets", "Desc", false, ownerJpa.getId());
        WorkspaceJpaEntity workspaceJpa = new WorkspaceJpaEntity(wsDomain.getId());
        workspaceJpa.setName(wsDomain.getName());
        workspaceJpa.setOwner(ownerJpa);
        workspaceJpa.setCreatedAt(wsDomain.getCreatedAt());
        workspaceJpa.setUpdatedAt(wsDomain.getUpdatedAt());
        entityPersist(workspaceJpa);

        Project projectDomain = Project.create("Project for Comment Targets", "Desc", workspaceJpa.getId());
        ProjectJpaEntity projectJpa = new ProjectJpaEntity(projectDomain.getId());
        projectJpa.setName(projectDomain.getName());
        projectJpa.setWorkspace(workspaceJpa);
        projectJpa.setCreatedAt(projectDomain.getCreatedAt());
        projectJpa.setUpdatedAt(projectDomain.getUpdatedAt());
        entityPersist(projectJpa);

        // Create a Question as a target
        questionTargetJpa = new QuestionJpaEntity(UUID.randomUUID().toString());
        questionTargetJpa.setTitle("Question to be commented on");
        questionTargetJpa.setDescription("Desc");
        questionTargetJpa.setStatus(QuestionStatus.OPEN);
        questionTargetJpa.setAuthor(ownerJpa);
        questionTargetJpa.setProject(projectJpa);
        questionTargetJpa.setCreatedAt(Instant.now());
        questionTargetJpa.setUpdatedAt(Instant.now());
        entityPersist(questionTargetJpa);

        // Create a Note as another target
        noteTargetJpa = new NoteJpaEntity(UUID.randomUUID().toString());
        noteTargetJpa.setTitle("Note to be commented on");
        noteTargetJpa.setContent("Content");
        noteTargetJpa.setAuthor(ownerJpa);
        noteTargetJpa.setProject(projectJpa);
        noteTargetJpa.setVersion(1);
        noteTargetJpa.setCreatedAt(Instant.now());
        noteTargetJpa.setUpdatedAt(Instant.now());
        entityPersist(noteTargetJpa);

        flushAndClear();

        // 2. Create Comment Domain Models
        comment1OnQuestion = Comment.create(
                TargetType.QUESTION,
                questionTargetJpa.getId(),
                authorUserJpa.getId(),
                "This is the first comment on the question."
        );

        comment2OnQuestion = Comment.create(
                TargetType.QUESTION,
                questionTargetJpa.getId(),
                authorUserJpa.getId(),
                "This is another comment on the same question."
        );

        commentOnNote = Comment.create(
                TargetType.NOTE,
                noteTargetJpa.getId(),
                authorUserJpa.getId(),
                "This is a comment on the note."
        );
    }

    // --- Basic CRUD, findById, existsById tests are correct and remain unchanged ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a comment")
        void create_shouldSaveComment() {
            // Act
            assertDoesNotThrow(() -> create(comment1OnQuestion));

            // Assert
            CommentJpaEntity foundInDb = entityManager.find(CommentJpaEntity.class, comment1OnQuestion.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getContent()).isEqualTo(comment1OnQuestion.getContent());
            assertThat(foundInDb.getTargetType()).isEqualTo(TargetType.QUESTION);
            assertThat(foundInDb.getTargetId()).isEqualTo(questionTargetJpa.getId());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(authorUserJpa.getId());

            // Verify retrieval via persistence layer
            Optional<Comment> foundComment = commentPersistence.findById(comment1OnQuestion.getId());
            assertThat(foundComment).isPresent();
            assertThat(foundComment.get().getContent()).isEqualTo(comment1OnQuestion.getContent());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing comment")
        void update_shouldModifyExistingComment() {
            // Arrange
            create(comment1OnQuestion);
            sleep(10);

            // Build updated domain model
            Comment updatedDomainComment = Comment.build(
                    comment1OnQuestion.getId(),
                    comment1OnQuestion.getTargetType(),
                    comment1OnQuestion.getTargetId(),
                    comment1OnQuestion.getAuthorId(),
                    comment1OnQuestion.getCreatedAt(),
                    "Updated: This is the first comment.", // Change content
                    Instant.now() // New updatedAt
            );

            // Act
            update(updatedDomainComment);

            // Assert
            Optional<Comment> foundCommentOpt = commentPersistence.findById(comment1OnQuestion.getId());
            assertThat(foundCommentOpt).isPresent();
            Comment foundComment = foundCommentOpt.get();

            assertThat(foundComment.getContent()).isEqualTo("Updated: This is the first comment.");
            assertThat(foundComment.getUpdatedAt()).isAfter(comment1OnQuestion.getUpdatedAt());
        }
    }

    // --- findAllByTargetId tests are correct and remain unchanged ---
    @Nested
    @DisplayName("findAllByTargetId Method Tests")
    class FindAllByTargetIdTests {
        @BeforeEach
        void setUpFindAllByTargetId() {
            create(comment1OnQuestion); // Target: question
            create(comment2OnQuestion); // Target: question
            create(commentOnNote);      // Target: note
        }

        @Test
        @DisplayName("should return all comments for a specific question ID")
        void findAllByTargetId_forQuestion_shouldReturnMatchingComments() {
            Pagination<Comment> result = commentPersistence.findAllByTargetId(Page.of(0, 10), TargetType.QUESTION, questionTargetJpa.getId());

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Comment::getContent)
                    .containsExactlyInAnyOrder(comment1OnQuestion.getContent(), comment2OnQuestion.getContent());
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (Generic Search)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            create(comment1OnQuestion);
            create(comment2OnQuestion);
            create(commentOnNote);
        }

        @Test
        @DisplayName("should filter by a single term (e.g., content)")
        void findAll_filterByContent_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("content", "another comment"));
            Pagination<Comment> result = commentPersistence.findAll(query);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getId()).isEqualTo(comment2OnQuestion.getId());
        }

        @Test
        @DisplayName("should filter by multiple terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for a comment with targetType "QUESTION" AND content containing "first"
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "targetType", "QUESTION",
                    "content", "first"
            ));

            // Act
            Pagination<Comment> resultWithMatch = commentPersistence.findAll(queryWithMatch);

            // Assert: Should find exactly one comment: comment1OnQuestion
            assertThat(resultWithMatch.total()).isEqualTo(1);
            assertThat(resultWithMatch.items().get(0).getId()).isEqualTo(comment1OnQuestion.getId());

            // Arrange: Search for a comment with targetType "NOTE" AND content containing "first" (should be none)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), QueryType.AND, Map.of(
                    "targetType", "NOTE",
                    "content", "first"
            ));

            // Act
            Pagination<Comment> resultWithoutMatch = commentPersistence.findAll(queryWithoutMatch);

            // Assert: Should find no comments
            assertThat(resultWithoutMatch.total()).isZero();
            assertThat(resultWithoutMatch.items()).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));

            assertThatThrownBy(() -> commentPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }
    }

    // Helper methods
    private void create(Comment entity) {
        commentPersistence.create(entity);
        flushAndClear();
    }

    private void update(Comment entity) {
        commentPersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        commentPersistence.deleteById(id);
        flushAndClear();
    }
}
