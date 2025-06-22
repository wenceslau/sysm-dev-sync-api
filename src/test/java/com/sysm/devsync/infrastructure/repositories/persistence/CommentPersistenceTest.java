package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
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

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a comment")
        void create_shouldSaveComment() {
            // Act
            assertDoesNotThrow(() -> commentPersistence.create(comment1OnQuestion));
            flushAndClear();


            // Assert
            CommentJpaEntity foundInDb = entityManager.find(CommentJpaEntity.class, comment1OnQuestion.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getContent()).isEqualTo(comment1OnQuestion.getContent());
            assertThat(foundInDb.getTargetType()).isEqualTo(TargetType.QUESTION);
            assertThat(foundInDb.getTargetId()).isEqualTo(questionTargetJpa.getId());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(authorUserJpa.getId());
            assertThat(foundInDb.getCreatedAt()).isEqualTo(comment1OnQuestion.getCreatedAt());

            // Verify retrieval via persistence layer
            Optional<Comment> foundComment = commentPersistence.findById(comment1OnQuestion.getId());
            assertThat(foundComment).isPresent();
            assertThat(foundComment.get().getContent()).isEqualTo(comment1OnQuestion.getContent());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when creating with null model")
        void create_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> commentPersistence.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Comment model must not be null");
        }

        @Test
        @DisplayName("should fail to create comment with non-existent Author ID due to FK constraint")
        void create_nonExistentAuthorId_shouldFail() {
            Comment commentWithInvalidAuthor = Comment.create(TargetType.QUESTION, questionTargetJpa.getId(), UUID.randomUUID().toString(), "Content");
            assertThatThrownBy(() -> {
                commentPersistence.create(commentWithInvalidAuthor);
                flushAndClear();
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing comment")
        void update_shouldModifyExistingComment() {
            // Arrange
            commentPersistence.create(comment1OnQuestion);
            flushAndClear();

            sleep(100);

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
            assertDoesNotThrow(() -> commentPersistence.update(updatedDomainComment));
            flushAndClear();


            // Assert
            Optional<Comment> foundCommentOpt = commentPersistence.findById(comment1OnQuestion.getId());
            assertThat(foundCommentOpt).isPresent();
            Comment foundComment = foundCommentOpt.get();

            assertThat(foundComment.getContent()).isEqualTo("Updated: This is the first comment.");
            assertThat(foundComment.getCreatedAt().truncatedTo(ChronoUnit.MILLIS))
                    .isEqualTo(comment1OnQuestion.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
            assertThat(foundComment.getUpdatedAt()).isAfter(comment1OnQuestion.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("findAllByTargetId Method Tests")
    class FindAllByTargetIdTests {
        @BeforeEach
        void setUpFindAllByTargetId() {
            commentPersistence.create(comment1OnQuestion); // Target: question
            commentPersistence.create(comment2OnQuestion); // Target: question
            commentPersistence.create(commentOnNote);      // Target: note
            flushAndClear();
        }

        @Test
        @DisplayName("should return all comments for a specific question ID")
        void findAllByTargetId_forQuestion_shouldReturnMatchingComments() {
            Pagination<Comment> result = commentPersistence.findAllByTargetId(Page.of(0, 10), TargetType.QUESTION, questionTargetJpa.getId());

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Comment::getContent)
                    .containsExactlyInAnyOrder(comment1OnQuestion.getContent(), comment2OnQuestion.getContent());
        }

        @Test
        @DisplayName("should return all comments for a specific note ID")
        void findAllByTargetId_forNote_shouldReturnMatchingComments() {
            Pagination<Comment> result = commentPersistence.findAllByTargetId(Page.of(0, 10), TargetType.NOTE, noteTargetJpa.getId());

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getContent()).isEqualTo(commentOnNote.getContent());
        }

        @Test
        @DisplayName("should return an empty page if no comments for target ID")
        void findAllByTargetId_noMatches_shouldReturnEmptyPage() {
            // Create a new question that has no comments
            QuestionJpaEntity newQuestion = new QuestionJpaEntity(UUID.randomUUID().toString());
            newQuestion.setTitle("A question with no comments");
            newQuestion.setDescription("");
            newQuestion.setAuthor(authorUserJpa);
            newQuestion.setProject(questionTargetJpa.getProject());
            newQuestion.setCreatedAt(Instant.now());
            newQuestion.setUpdatedAt(Instant.now());
            newQuestion.setStatus(QuestionStatus.OPEN);
            entityManager.persist(newQuestion);

            flushAndClear();

            Pagination<Comment> result = commentPersistence.findAllByTargetId(Page.of(0, 10), TargetType.QUESTION, newQuestion.getId());

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null arguments")
        void findAllByTargetId_nullArgs_shouldThrowException() {
            assertThatThrownBy(() -> commentPersistence.findAllByTargetId(Page.of(0, 10), null, "some-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Target type must not be null");

            assertThatThrownBy(() -> commentPersistence.findAllByTargetId(Page.of(0, 10), TargetType.NOTE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Target ID must not be null or empty");
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (AbstractPersistence)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            commentPersistence.create(comment1OnQuestion);
            commentPersistence.create(comment2OnQuestion);
            commentPersistence.create(commentOnNote);
            flushAndClear();
        }

        @Test
        @DisplayName("should filter by content")
        void findAll_filterByContent_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "content=another comment");
            Pagination<Comment> result = commentPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getContent()).isEqualTo(comment2OnQuestion.getContent());
        }

        @Test
        @DisplayName("should filter by targetType")
        void findAll_filterByTargetType_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "targetType=NOTE");
            Pagination<Comment> result = commentPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getContent()).isEqualTo(commentOnNote.getContent());
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "invalidField=test");

            assertThatThrownBy(() -> commentPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }
    }
}
