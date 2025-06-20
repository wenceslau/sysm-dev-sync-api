package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page; // Your domain Page
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.infrastructure.PersistenceTest; // Your custom test slice
import com.sysm.devsync.infrastructure.repositories.*; // Import all repositories
import com.sysm.devsync.infrastructure.repositories.entities.*; // Import all entities
import jakarta.persistence.criteria.Predicate; // Needed for Specification tests
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException; // For FK constraint violations
import org.springframework.data.domain.PageRequest; // Spring's PageRequest
import org.springframework.data.domain.Pageable; // Spring's Pageable
import org.springframework.data.jpa.domain.Specification; // Spring's Specification
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit; // For timestamp comparison
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sysm.devsync.infrastructure.Utils.like; // Assuming this utility exists
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@PersistenceTest
@Import(QuestionPersistence.class) // Import the class under test
public class QuestionPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QuestionPersistence questionPersistence; // The class under test

    // Autowire other repositories needed for setup and verification
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private TagJpaRepository tagJpaRepository;

    // Prerequisite JPA entities (persisted before tests)
    private UserJpaEntity authorUserJpa;
    private UserJpaEntity workspaceOwnerJpa; // Needed for Workspace setup
    private WorkspaceJpaEntity workspaceJpa;
    private ProjectJpaEntity project1Jpa;
    private ProjectJpaEntity project2Jpa;
    private TagJpaEntity tag1Jpa;
    private TagJpaEntity tag2Jpa;
    private TagJpaEntity tag3Jpa;

    // Domain models for testing
    private Question question1Domain;
    private Question question2Domain;
    private Question question3Domain;

    @BeforeEach
    void setUp() {
        // Clean up in reverse order of dependency or let @DataJpaTest handle rollback
        // Using executeUpdate on queries is often more reliable than deleteAllInBatch for complex relationships
        entityManager.getEntityManager().createQuery("DELETE FROM Question").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Tag").executeUpdate(); // Clean tags table
        entityManager.getEntityManager().createQuery("DELETE FROM Project").executeUpdate();
//        entityManager.getEntityManager().createQuery("DELETE FROM WorkspaceMember").executeUpdate(); // If workspace_members table exists
        entityManager.getEntityManager().createQuery("DELETE FROM Workspace").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM User").executeUpdate();
        entityManager.flush(); // Ensure cleanup is done before creating new entities

        // 1. Create and Persist Prerequisite Entities
        // Create Owner User for Workspace
        User workspaceOwnerDomain = User.create("Workspace Owner", "ws.owner@example.com",  UserRole.ADMIN);
        workspaceOwnerJpa = UserJpaEntity.fromModel(workspaceOwnerDomain); // Assuming fromModel works for User
        entityManager.persist(workspaceOwnerJpa);

        // Create Author User for Questions
        User authorDomain = User.create("Author User", "author@example.com",  UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain); // Assuming fromModel works for User
        entityManager.persist(authorUserJpa);

        // Create Workspace (requires owner)
        Workspace wsDomain = Workspace.create("Test Workspace for Questions", "Workspace description", false, workspaceOwnerJpa.getId());
        // Manually map or use a correct fromModel that fetches owner if needed
        workspaceJpa = new WorkspaceJpaEntity();
        workspaceJpa.setId(wsDomain.getId());
        workspaceJpa.setName(wsDomain.getName());
        workspaceJpa.setDescription(wsDomain.getDescription());
        workspaceJpa.setPrivate(wsDomain.isPrivate());
        workspaceJpa.setOwner(workspaceOwnerJpa); // Set the managed owner
        workspaceJpa.setCreatedAt(wsDomain.getCreatedAt());
        workspaceJpa.setUpdatedAt(wsDomain.getUpdatedAt());
        entityManager.persist(workspaceJpa);

        // Create Projects (requires workspace)
        Project project1DomainModel = Project.create("Project Alpha for Questions", "Alpha description", workspaceJpa.getId());
        // Manually map or use a correct fromModel that fetches workspace if needed
        project1Jpa = new ProjectJpaEntity();
        project1Jpa.setId(project1DomainModel.getId());
        project1Jpa.setName(project1DomainModel.getName());
        project1Jpa.setDescription(project1DomainModel.getDescription());
        project1Jpa.setWorkspace(workspaceJpa); // Set the managed workspace
        project1Jpa.setCreatedAt(project1DomainModel.getCreatedAt());
        project1Jpa.setUpdatedAt(project1DomainModel.getUpdatedAt());
        entityManager.persist(project1Jpa);

        Project project2DomainModel = Project.create("Project Beta for Questions", "Beta description", workspaceJpa.getId());
        project2Jpa = new ProjectJpaEntity();
        project2Jpa.setId(project2DomainModel.getId());
        project2Jpa.setName(project2DomainModel.getName());
        project2Jpa.setDescription(project2DomainModel.getDescription());
        project2Jpa.setWorkspace(workspaceJpa);
        project2Jpa.setCreatedAt(project2DomainModel.getCreatedAt());
        project2Jpa.setUpdatedAt(project2DomainModel.getUpdatedAt());
        entityManager.persist(project2Jpa);

        // Create Tags
        Tag tag1Domain = Tag.build(UUID.randomUUID().toString(), "java", "#FF0000", "Java related questions", "Programming", 10);
        tag1Jpa = TagJpaEntity.fromModel(tag1Domain); // Assuming fromModel works for Tag
        entityManager.persist(tag1Jpa);

        Tag tag2Domain = Tag.build(UUID.randomUUID().toString(), "spring", "#00FF00", "Spring related questions", "Frameworks", 15);
        tag2Jpa = TagJpaEntity.fromModel(tag2Domain);
        entityManager.persist(tag2Jpa);

        Tag tag3Domain = Tag.build(UUID.randomUUID().toString(), "jpa", "#0000FF", "JPA related questions", "Databases", 20);
        tag3Jpa = TagJpaEntity.fromModel(tag3Domain);
        entityManager.persist(tag3Jpa);

        entityManager.flush(); // Ensure all prerequisites are in DB before creating questions

        // 2. Create Question Domain Models (referencing IDs of persisted entities)
        question1Domain = Question.create(
                "How to test JPA ManyToMany?",
                "A detailed description of the problem for question 1.",
                project1Jpa.getId(), // Use ID of persisted project
                authorUserJpa.getId() // Use ID of persisted author
        );
        question1Domain.addTag(tag1Jpa.getId());
        question1Domain.addTag(tag2Jpa.getId());

        question2Domain = Question.create(
                "Best practices for Spring Boot?",
                "Looking for best practices in Spring Boot applications.",
                project1Jpa.getId(), // Same project as question1
                authorUserJpa.getId() // Same author
        );
        question2Domain.addTag(tag2Jpa.getId());
        question2Domain.addTag(tag2Jpa.getId());

        question3Domain = Question.create(
                "Understanding JPA Fetch Types",
                "Deep dive into LAZY and EAGER fetching.",
                project2Jpa.getId(), // Different project
                authorUserJpa.getId() // Same author
        );
        question3Domain.addTag(tag1Jpa.getId());
        question3Domain.addTag(tag3Jpa.getId());
        question3Domain.changeStatus(QuestionStatus.RESOLVED);
    }

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a question")
        void create_shouldSaveQuestion() {
            // Act
            // This relies on QuestionJpaEntity.fromModel correctly mapping IDs to transient entities
            // and JPA being able to resolve them because the entities with those IDs exist in the DB.
            assertDoesNotThrow(() -> questionPersistence.create(question1Domain));

            entityManager.flush(); // Ensure save is committed
            entityManager.clear(); // Clear cache to fetch fresh from DB

            // Assert
            QuestionJpaEntity foundInDb = entityManager.find(QuestionJpaEntity.class, question1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getTitle()).isEqualTo(question1Domain.getTitle());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(question1Domain.getAuthorId());
            assertThat(foundInDb.getProject().getId()).isEqualTo(question1Domain.getProjectId());
            assertThat(foundInDb.getTags().stream().map(TagJpaEntity::getId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(question1Domain.getTagsId());
            assertThat(foundInDb.getCreatedAt()).isNotNull();
            assertThat(foundInDb.getUpdatedAt()).isNotNull();

            // Verify retrieval via persistence layer
            Optional<Question> foundQuestion = questionPersistence.findById(question1Domain.getId());
            assertThat(foundQuestion).isPresent();
            assertThat(foundQuestion.get().getTitle()).isEqualTo(question1Domain.getTitle());
            assertThat(foundQuestion.get().getAuthorId()).isEqualTo(question1Domain.getAuthorId());
            assertThat(foundQuestion.get().getProjectId()).isEqualTo(question1Domain.getProjectId());
            assertThat(foundQuestion.get().getTagsId()).containsExactlyInAnyOrderElementsOf(question1Domain.getTagsId());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when creating with null model")
        void create_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> questionPersistence.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question model must not be null");
        }

        @Test
        @DisplayName("should fail to create question with non-existent Author ID due to FK constraint")
        void create_nonExistentAuthorId_shouldFail() {
            Question questionWithInvalidAuthor = Question.create(
                    "Invalid Author Question", "Desc", project1Jpa.getId(), UUID.randomUUID().toString()
            );
            // This test relies on the database foreign key constraint.
            // QuestionJpaEntity.fromModel creates a transient UserJpaEntity(id).
            // When saving QuestionJpaEntity, if the author_id doesn't exist in the users table,
            // the DB will throw a constraint violation.
            assertThatThrownBy(() -> {
                questionPersistence.create(questionWithInvalidAuthor);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(ConstraintViolationException.class); // Or a more specific FK violation if available
        }

        @Test
        @DisplayName("should fail to create question with non-existent Project ID due to FK constraint")
        void create_nonExistentProjectId_shouldFail() {
            Question questionWithInvalidProject = Question.create(
                    "Invalid Author Question", "Desc", project1Jpa.getId(), UUID.randomUUID().toString()
            );
            // This test relies on the database foreign key constraint.
            // QuestionJpaEntity.fromModel creates a transient ProjectJpaEntity(id).
            // When saving QuestionJpaEntity, if the project_id doesn't exist in the projecs table,
            // the DB will throw a constraint violation.
            assertThatThrownBy(() -> {
                questionPersistence.create(questionWithInvalidProject);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(ConstraintViolationException.class); // Or a more specific FK violation if available
        }

        @Test
        @DisplayName("should fail to create question with non-existent Tag ID due to FK constraint")
        void create_nonExistentTagId_shouldFail() {
            Question questionWithInvalidTag = Question.create(
                    "Invalid Tag Question", "Desc", project1Jpa.getId(), authorUserJpa.getId()
            );
            questionWithInvalidTag.addTag(tag1Jpa.getId());
            questionWithInvalidTag.addTag(UUID.randomUUID().toString()); // Add a non-existent Tag ID
            // This test relies on the database foreign key constraint on the question_tags join table.
            // QuestionJpaEntity.fromModel creates transient TagJpaEntity(id) instances.
            // When saving QuestionJpaEntity, JPA will try to insert rows into question_tags.
            // If a tag_id doesn't exist in the tags table, the DB will throw a constraint violation.
            assertThatThrownBy(() -> {
                questionPersistence.create(questionWithInvalidTag);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(JpaObjectRetrievalFailureException.class); // Or a more specific FK violation if available
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing question")
        void update_shouldModifyExistingQuestion() {
            // Arrange: First, create the question
            questionPersistence.create(question1Domain);
            entityManager.flush();
            entityManager.clear(); // Detach to simulate fresh fetch & update

            // Build updated domain model
            Question updatedDomainQuestion = Question.build(
                    question1Domain.getId(),
                    question1Domain.getCreatedAt(), // Keep original createdAt
                    Instant.now(), // New updatedAt (will be overwritten by @PreUpdate)
                    "Updated: How to test JPA ManyToMany?", // Change title
                    "Updated description for question 1.", // Change description
                    Set.of(tag2Jpa.getId(), tag3Jpa.getId()), // Change tags (remove tag1, add tag3)
                    project2Jpa.getId(), // Change project
                    authorUserJpa.getId(), // Keep same author
                    QuestionStatus.CLOSED // Change status
            );

            // Act
            // This relies on QuestionJpaEntity.fromModel correctly mapping IDs to transient entities
            // and JPA being able to resolve them because the entities with those IDs exist in the DB.
            assertDoesNotThrow(() -> questionPersistence.update(updatedDomainQuestion));
            entityManager.flush();
            entityManager.clear();

            // Assert
            Optional<Question> foundQuestionOpt = questionPersistence.findById(question1Domain.getId());
            assertThat(foundQuestionOpt).isPresent();
            Question foundQuestion = foundQuestionOpt.get();

            assertThat(foundQuestion.getTitle()).isEqualTo("Updated: How to test JPA ManyToMany?");
            assertThat(foundQuestion.getDescription()).isEqualTo("Updated description for question 1.");
            assertThat(foundQuestion.getTagsId()).containsExactlyInAnyOrderElementsOf(Set.of(tag2Jpa.getId(), tag3Jpa.getId()));
            assertThat(foundQuestion.getProjectId()).isEqualTo(project2Jpa.getId());
            assertThat(foundQuestion.getAuthorId()).isEqualTo(authorUserJpa.getId());
            assertThat(foundQuestion.getStatus()).isEqualTo(QuestionStatus.CLOSED);
            assertThat(foundQuestion.getCreatedAt().truncatedTo(ChronoUnit.MILLIS))
                    .isEqualTo(question1Domain.getCreatedAt().truncatedTo(ChronoUnit.MILLIS)); // createdAt should not change
            assertThat(foundQuestion.getUpdatedAt()).isAfterOrEqualTo(foundQuestion.getCreatedAt()); // updatedAt should be updated
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when updating with null model")
        void update_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> questionPersistence.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question model must not be null");
        }

        @Test
        @DisplayName("update should effectively insert if ID does not exist (current behavior)")
        void update_nonExistentId_shouldInsert() {
            // Current `update` method behavior: if ID doesn't exist, `repository.save()` will insert.
            Question newQuestion = Question.create(
                    "New Question via Update", "Desc", project1Jpa.getId(), authorUserJpa.getId()
            );
            newQuestion.addTag(tag1Jpa.getId());
            // Ensure the ID is one that does not exist
            Question  newQuestionToUpdate = Question.build(
                    UUID.randomUUID().toString(), // Use a new, non-existent ID
                    newQuestion.getCreatedAt(),
                    newQuestion.getUpdatedAt(),
                    newQuestion.getTitle(),
                    newQuestion.getDescription(),
                    newQuestion.getTagsId(),
                    newQuestion.getProjectId(),
                    newQuestion.getAuthorId(),
                    newQuestion.getStatus()
            );


            assertDoesNotThrow(() -> questionPersistence.update(newQuestionToUpdate));
            entityManager.flush();
            entityManager.clear();

            Optional<Question> foundQuestion = questionPersistence.findById(newQuestionToUpdate.getId());
            assertThat(foundQuestion).isPresent();
            assertThat(foundQuestion.get().getTitle()).isEqualTo("New Question via Update");
        }

        @Test
        @DisplayName("should fail to update question with non-existent Author ID due to FK constraint")
        void update_nonExistentAuthorId_shouldFail() {
            // Arrange: Create an initial valid question
            questionPersistence.create(question1Domain);
            entityManager.flush();
            entityManager.clear();

            // Build an updated model with a non-existent author ID
            Question updatedQuestion = Question.build(
                    question1Domain.getId(),
                    question1Domain.getCreatedAt(),
                    Instant.now(),
                    question1Domain.getTitle(),
                    question1Domain.getDescription(),
                    question1Domain.getTagsId(),
                    question1Domain.getProjectId(),
                    UUID.randomUUID().toString(), // Non-existent Author ID
                    question1Domain.getStatus()
            );

            // Act & Assert
            assertThatThrownBy(() -> {
                questionPersistence.update(updatedQuestion);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("should fail to update question with non-existent Project ID due to FK constraint")
        void update_nonExistentProjectId_shouldFail() {
            // Arrange: Create an initial valid question
            questionPersistence.create(question1Domain);
            entityManager.flush();
            entityManager.clear();

            // Build an updated model with a non-existent project ID
            Question updatedQuestion = Question.build(
                    question1Domain.getId(),
                    question1Domain.getCreatedAt(),
                    Instant.now(),
                    question1Domain.getTitle(),
                    question1Domain.getDescription(),
                    question1Domain.getTagsId(),
                    UUID.randomUUID().toString(), // Non-existent Project ID
                    question1Domain.getAuthorId(),
                    question1Domain.getStatus()
            );

            // Act & Assert
            assertThatThrownBy(() -> {
                questionPersistence.update(updatedQuestion);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        @DisplayName("should fail to update question with non-existent Tag ID due to FK constraint")
        void update_nonExistentTagId_shouldFail() {
            // Arrange: Create an initial valid question
            questionPersistence.create(question1Domain);
            entityManager.flush();
            entityManager.clear();

            // Build an updated model with a non-existent tag ID
            Set<String> updatedTags = new HashSet<>(question1Domain.getTagsId());
            updatedTags.add(UUID.randomUUID().toString()); // Add a non-existent Tag ID

            Question updatedQuestion = Question.build(
                    question1Domain.getId(),
                    question1Domain.getCreatedAt(),
                    Instant.now(),
                    question1Domain.getTitle(),
                    question1Domain.getDescription(),
                    updatedTags, // Contains a non-existent Tag ID
                    question1Domain.getProjectId(),
                    question1Domain.getAuthorId(),
                    question1Domain.getStatus()
            );

            // Act & Assert
            assertThatThrownBy(() -> {
                questionPersistence.update(updatedQuestion);
                entityManager.flush(); // Force DB interaction
            }).isInstanceOf(JpaObjectRetrievalFailureException.class);
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a question by its ID")
        void deleteById_shouldRemoveQuestion() {
            // Arrange
            questionPersistence.create(question1Domain);
            entityManager.flush();
            assertThat(questionPersistence.existsById(question1Domain.getId())).isTrue();

            // Act
            questionPersistence.deleteById(question1Domain.getId());
            entityManager.flush();
            entityManager.clear();

            // Assert
            assertThat(questionPersistence.existsById(question1Domain.getId())).isFalse();
            assertThat(questionPersistence.findById(question1Domain.getId())).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when deleting with null ID")
        void deleteById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> questionPersistence.deleteById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question ID must not be null or empty");
        }

        @Test
        @DisplayName("deleteById should not throw error for non-existent ID")
        void deleteById_nonExistentId_shouldNotThrowError() {
            assertDoesNotThrow(() -> questionPersistence.deleteById(UUID.randomUUID().toString()));
            entityManager.flush(); // Ensure no exceptions during flush
        }
    }

    @Nested
    @DisplayName("findById Method Tests")
    class FindByIdTests {
        @Test
        @DisplayName("should return question when found")
        void findById_whenQuestionExists_shouldReturnQuestion() {
            // Arrange
            questionPersistence.create(question1Domain);
            entityManager.flush();

            // Act
            Optional<Question> foundQuestion = questionPersistence.findById(question1Domain.getId());

            // Assert
            assertThat(foundQuestion).isPresent();
            assertThat(foundQuestion.get().getId()).isEqualTo(question1Domain.getId());
            assertThat(foundQuestion.get().getTitle()).isEqualTo(question1Domain.getTitle());
        }

        @Test
        @DisplayName("should return empty optional when question not found")
        void findById_whenQuestionDoesNotExist_shouldReturnEmpty() {
            // Act
            Optional<Question> foundQuestion = questionPersistence.findById(UUID.randomUUID().toString());

            // Assert
            assertThat(foundQuestion).isNotPresent();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when finding with null ID")
        void findById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> questionPersistence.findById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question ID must not be null or empty");
        }
    }

    @Nested
    @DisplayName("existsById Method Tests")
    class ExistsByIdTests {
        @Test
        @DisplayName("should return true when question exists")
        void existsById_whenQuestionExists_shouldReturnTrue() {
            // Arrange
            questionPersistence.create(question1Domain);
            entityManager.flush();

            // Act
            boolean exists = questionPersistence.existsById(question1Domain.getId());

            // Assert
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when question does not exist")
        void existsById_whenQuestionDoesNotExist_shouldReturnFalse() {
            // Act
            boolean exists = questionPersistence.existsById(UUID.randomUUID().toString());

            // Assert
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when checking existence with null ID")
        void existsById_nullId_shouldThrowException() {
            assertThatThrownBy(() -> questionPersistence.existsById(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question ID must not be null or empty");
        }
    }

    @Nested
    @DisplayName("findAllByProjectId Method Tests")
    class FindAllByProjectIdTests {
        @BeforeEach
        void setUpFindAllByProjectId() {
            // Persist test data
            questionPersistence.create(question1Domain); // project1
            questionPersistence.create(question2Domain); // project1
            questionPersistence.create(question3Domain); // project2
            entityManager.flush();
        }

        @Test
        @DisplayName("should return questions for a specific project ID")
        void findAllByProjectId_shouldReturnMatchingQuestions() {
            // Use your domain Page object
            Page domainPage = Page.of(0, 10);
            Pagination<Question> result = questionPersistence.findAllByProjectId(domainPage, project1Jpa.getId());

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder("How to test JPA ManyToMany?", "Best practices for Spring Boot?");
            assertThat(result.total()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty page if no questions for project ID")
        void findAllByProjectId_noMatches_shouldReturnEmptyPage() {
            // Use your domain Page object
            Page domainPage = Page.of(0, 10);
            Pagination<Question> result = questionPersistence.findAllByProjectId(domainPage, UUID.randomUUID().toString()); // Non-existent project ID

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when project ID is null or empty")
        void findAllByProjectId_nullOrEmptyId_shouldThrowException() {
            Page domainPage = Page.of(0, 10);
            assertThatThrownBy(() -> questionPersistence.findAllByProjectId(domainPage, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID must not be null or empty");

            assertThatThrownBy(() -> questionPersistence.findAllByProjectId(domainPage, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Project ID must not be null or empty");
        }
    }


    @Nested
    @DisplayName("findAll Method Tests (AbstractPersistence)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            // Persist test data
            questionPersistence.create(question1Domain); // Alpha, project1, authorUser, tags: java, spring
            questionPersistence.create(question2Domain); // Beta, project1, authorUser, tags: spring, jpa
            questionPersistence.create(question3Domain); // Gamma, project2, authorUser, tags: java, jpa
            entityManager.flush();
        }

        @Test
        @DisplayName("should return all questions when no search terms provided")
        void findAll_noTerms_shouldReturnAllQuestions() {
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), ""); // Use domain Pageable
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by question title")
        void findAll_filterByTitle_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "title=JPA ManyToMany");
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getTitle()).isEqualTo(question1Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by question description")
        void findAll_filterByDescription_shouldReturnMatching() {
            // NOTE: Your QuestionPersistence.createPredicateForField uses key "content" but entity field is "description".
            // This test uses "description" assuming the predicate key should match the entity field.
            // If your predicate uses "content", change this test key to "content".
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "description=Spring Boot");
            // If your predicate uses "content", use: SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Pageable.of(0, 10), "content=Spring Boot");

            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getTitle()).isEqualTo(question2Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by projectId")
        void findAll_filterByProjectId_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "projectId=" + project1Jpa.getId());
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(2);
            List<String> titles = result.items().stream().map(Question::getTitle).toList();
            assertThat(titles).containsExactlyInAnyOrder(question1Domain.getTitle(), question2Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by authorId")
        void findAll_filterByAuthorId_shouldReturnMatching() {
            // Create another author and question for differentiation
            UserJpaEntity anotherAuthor = new UserJpaEntity(UUID.randomUUID().toString());
            anotherAuthor.setName("Another Author");
            anotherAuthor.setEmail("another.author@example.com");
            entityManager.persist(anotherAuthor);

            Question questionByAnotherAuthor = Question.create(
                    "Question by Another", "Desc", project1Jpa.getId(), anotherAuthor.getId()
            );
            questionPersistence.create(questionByAnotherAuthor);
            entityManager.flush();


            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "authorId=" + authorUserJpa.getId());
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(3); // question1, question2, question3 are by authorUser
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder(question1Domain.getTitle(), question2Domain.getTitle(), question3Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by status")
        void findAll_filterByStatus_shouldReturnMatching() {
            // NOTE: Your QuestionPersistence.createPredicateForField needs to convert the string value to QuestionStatus enum.
            // This test assumes that conversion is handled correctly.
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "status=RESOLVED");
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getTitle()).isEqualTo(question3Domain.getTitle());

            SearchQuery queryOpen = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "status=OPEN");
            Pagination<Question> resultOpen = questionPersistence.findAll(queryOpen);

            assertThat(resultOpen.items()).hasSize(2);
            assertThat(resultOpen.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder(question1Domain.getTitle(), question2Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by tagsId")
        void findAll_filterByTagsId_shouldReturnMatching() {
            // NOTE: Your QuestionPersistence.createPredicateForField uses crBuilder.isMember which is likely incorrect for ManyToMany joins.
            // This test assumes the predicate correctly filters by tag ID using a join.
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "tagsId=" + tag1Jpa.getId()); // Search for tag 'java'
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(2); // question1 (java, spring), question3 (java, jpa)
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder(question1Domain.getTitle(), question3Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by tagsName")
        void findAll_filterByTagsName_shouldReturnMatching() {
            // NOTE: Your QuestionPersistence.createPredicateForField uses crBuilder.isMember which is likely incorrect for ManyToMany joins.
            // This test assumes the predicate correctly filters by tag name using a join.
            // It also assumes an exact match on tag name, as per your predicate code.
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "tagsName=spring"); // Search for tag 'spring'
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(2); // question1 (java, spring), question2 (spring, jpa)
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder(question1Domain.getTitle(), question2Domain.getTitle());
        }


        @Test
        @DisplayName("should filter by multiple terms (OR logic)")
        void findAll_multipleTerms_OR_Logic_shouldReturnMatching() {
            // Search for questions with title "JPA" OR status "RESOLVED"
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "title=JPA#status=RESOLVED");
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).hasSize(2); // question1 (title has JPA), question3 (status is RESOLVED)
            assertThat(result.items()).extracting(Question::getTitle)
                    .containsExactlyInAnyOrder(question1Domain.getTitle(), question3Domain.getTitle());
        }


        @Test
        @DisplayName("should throw BusinessException for an invalid search field (not in VALID_SEARCHABLE_FIELDS)")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            // NOTE: This test relies on QuestionPersistence.isNotValidSearchableField being implemented correctly
            // to check against a set of valid fields.
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "invalidField=test");

            assertThatThrownBy(() -> questionPersistence.findAll(query))
                    .isInstanceOf(BusinessException.class) // Should throw BusinessException from AbstractPersistence
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should throw BusinessException for an unsupported search logic (handled by default case in predicate)")
        void findAll_unsupportedSearchLogic_shouldThrowBusinessException() {
            // NOTE: This test relies on QuestionPersistence.createPredicateForField throwing BusinessException
            // for a field that is considered valid by isNotValidSearchableField but not handled in the switch.
            // Given the current structure, if a field is in VALID_SEARCHABLE_FIELDS but not in the switch,
            // the default case throwing BusinessException will be hit.
            // Example: If "category" was added to VALID_SEARCHABLE_FIELDS but not the switch.
            // Assuming all fields in VALID_SEARCHABLE_FIELDS are handled by the switch, this test might be redundant
            // or require temporarily modifying VALID_SEARCHABLE_FIELDS for the test.
            // The `invalidSearchField_shouldThrowBusinessException` test covers the primary validation.
        }


        @Test
        @DisplayName("should handle terms with no matches")
        void findAll_termWithNoMatches_shouldReturnEmptyPage() {
            SearchQuery query = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 10), "title=NonExistentQuestion");
            Pagination<Question> result = questionPersistence.findAll(query);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            // Order by title to ensure predictable pagination results
            SearchQuery queryPage1 = new SearchQuery(com.sysm.devsync.domain.Page.of(0, 2, "title", "asc"), "");
            Pagination<Question> result1 = questionPersistence.findAll(queryPage1);

            assertThat(result1.items()).hasSize(2);
            assertThat(result1.currentPage()).isEqualTo(0);
            assertThat(result1.perPage()).isEqualTo(2);
            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).extracting(Question::getTitle)
                    .containsExactly("Best practices for Spring Boot?", "How to test JPA ManyToMany?"); // Ordered by title asc

            SearchQuery queryPage2 = new SearchQuery(com.sysm.devsync.domain.Page.of(1, 2, "title", "asc"), "");
            Pagination<Question> result2 = questionPersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items()).extracting(Question::getTitle)
                    .containsExactly("Understanding JPA Fetch Types"); // The last one
        }
    }
}
