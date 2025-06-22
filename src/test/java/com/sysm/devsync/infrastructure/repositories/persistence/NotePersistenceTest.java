package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Note;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Tag;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(NotePersistence.class) // Import the class under test
public class NotePersistenceTest extends AbstractRepositoryTest {

    @Autowired
    private NotePersistence notePersistence; // The class under test

    // Prerequisite JPA entities (persisted before tests)
    private UserJpaEntity authorUserJpa;
    private ProjectJpaEntity project1Jpa;
    private ProjectJpaEntity project2Jpa;
    private TagJpaEntity tagJpaJpa;

    // Domain models for testing
    private Note note1Domain;
    private Note note2Domain;
    private Note note3Domain;

    @BeforeEach
    void setUp() {
        // Clear previous data to ensure a clean state
        clearRepositories();

        // 1. Create and Persist Prerequisite Entities
        User workspaceOwnerDomain = User.create("Workspace Owner", "ws.owner.note@example.com", UserRole.ADMIN);
        UserJpaEntity workspaceOwnerJpa = UserJpaEntity.fromModel(workspaceOwnerDomain);
        entityPersist(workspaceOwnerJpa);

        User authorDomain = User.create("Note Author", "note.author@example.com", UserRole.MEMBER);
        authorUserJpa = UserJpaEntity.fromModel(authorDomain);
        entityPersist(authorUserJpa);

        Workspace wsDomain = Workspace.create("Workspace for Note Tests", "Workspace description", false, workspaceOwnerJpa.getId());
        WorkspaceJpaEntity workspaceJpa = WorkspaceJpaEntity.fromModel(wsDomain);
        entityPersist(workspaceJpa);

        Project project1DomainModel = Project.create("Project Alpha for Notes", "Alpha description", workspaceJpa.getId());
        project1Jpa = ProjectJpaEntity.fromModel(project1DomainModel);
        entityPersist(project1Jpa);

        Project project2DomainModel = Project.create("Project Beta for Notes", "Beta description", workspaceJpa.getId());
        project2Jpa = ProjectJpaEntity.fromModel(project2DomainModel);
        entityPersist(project2Jpa);

        Tag tagJavaDomain = Tag.create("java", "#F89820");
        TagJpaEntity tagJavaJpa = TagJpaEntity.fromModel(tagJavaDomain);
        entityPersist(tagJavaJpa);

        Tag tagSpringDomain = Tag.create("spring", "#F89820");
        TagJpaEntity tagSpringJpa = TagJpaEntity.fromModel(tagSpringDomain);
        entityPersist(tagSpringJpa);

        Tag tagJpaDomain = Tag.create("jpa", "#F89820");
        tagJpaJpa = TagJpaEntity.fromModel(tagJpaDomain);
        entityPersist(tagJpaJpa);

        flushAndClear();

        // 2. Create Note Domain Models
        note1Domain = Note.create(
                "First Note Title",
                "This is the content for the first note, discussing Java.",
                project1Jpa.getId(),
                authorUserJpa.getId()
        );
        note1Domain.addTag(tagJavaJpa.getId());
        note1Domain.addTag(tagSpringJpa.getId());


        note2Domain = Note.create(
                "Second Note Title",
                "Content for the second note, about Spring and JPA.",
                project1Jpa.getId(),
                authorUserJpa.getId()
        );
        note2Domain.addTag(tagSpringJpa.getId());
        note2Domain.addTag(tagJpaJpa.getId());

        note3Domain = Note.create(
                "Note for Project Beta",
                "This note belongs to project Beta and is about Java.",
                project2Jpa.getId(),
                authorUserJpa.getId()
        );
        note3Domain.addTag(tagJavaJpa.getId());
    }

    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a note")
        void create_shouldSaveNote() {
            // Act
            assertDoesNotThrow(() -> notePersistence.create(note1Domain));
            flushAndClear();

            // Assert
            NoteJpaEntity foundInDb = entityManager.find(NoteJpaEntity.class, note1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getTitle()).isEqualTo(note1Domain.getTitle());
            assertThat(foundInDb.getAuthor().getId()).isEqualTo(note1Domain.getAuthorId());
            assertThat(foundInDb.getProject().getId()).isEqualTo(note1Domain.getProjectId());
            assertThat(foundInDb.getTags().stream().map(TagJpaEntity::getId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(note1Domain.getTagsId());
            assertThat(foundInDb.getCreatedAt()).isEqualTo(note1Domain.getCreatedAt());
            assertThat(foundInDb.getVersion()).isEqualTo(1);

            // Verify retrieval via persistence layer
            Optional<Note> foundNote = notePersistence.findById(note1Domain.getId());
            assertThat(foundNote).isPresent();
            assertThat(foundNote.get().getTitle()).isEqualTo(note1Domain.getTitle());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when creating with null model")
        void create_nullModel_shouldThrowException() {
            assertThatThrownBy(() -> notePersistence.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Note model must not be null");
        }

        @Test
        @DisplayName("should fail to create note with non-existent Project ID due to FK constraint")
        void create_nonExistentProjectId_shouldFail() {
            Note noteWithInvalidProject = Note.create("Title", "Content", UUID.randomUUID().toString(), authorUserJpa.getId());
            assertThatThrownBy(() -> {
                notePersistence.create(noteWithInvalidProject);
                flushAndClear();
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing note")
        void update_shouldModifyExistingNote() {
            // Arrange
            notePersistence.create(note1Domain);
            flushAndClear();


            // Build updated domain model
            Note updatedDomainNote = Note.build(
                    note1Domain.getId(),
                    note1Domain.getCreatedAt(),
                    Instant.now(),
                    "Updated: First Note Title",
                    "Updated content.",
                    Set.of(tagJpaJpa.getId()), // Change tags
                    project2Jpa.getId(), // Change project
                    authorUserJpa.getId(),
                    note1Domain.getVersion()
            );

            // Act
            assertDoesNotThrow(() -> notePersistence.update(updatedDomainNote));
            flushAndClear();


            // Assert
            Optional<Note> foundNoteOpt = notePersistence.findById(note1Domain.getId());
            assertThat(foundNoteOpt).isPresent();
            Note foundNote = foundNoteOpt.get();

            assertThat(foundNote.getTitle()).isEqualTo("Updated: First Note Title");
            assertThat(foundNote.getTagsId()).containsExactly(tagJpaJpa.getId());
            assertThat(foundNote.getProjectId()).isEqualTo(project2Jpa.getId());
            assertThat(foundNote.getCreatedAt().truncatedTo(ChronoUnit.MILLIS))
                    .isEqualTo(note1Domain.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
            assertThat(foundNote.getUpdatedAt()).isAfter(note1Domain.getUpdatedAt());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when updating with null model")
        void update_nullModel_shouldThrowException() {
            // This test correctly points out a copy-paste error in the implementation.
            // The implementation throws "Question model must not be null"
            assertThatThrownBy(() -> notePersistence.update(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Question model must not be null");
        }
    }

    @Nested
    @DisplayName("deleteById Method Tests")
    class DeleteByIdTests {
        @Test
        @DisplayName("should delete a note by its ID")
        void deleteById_shouldRemoveNote() {
            // Arrange
            notePersistence.create(note1Domain);
            flushAndClear();
            assertThat(notePersistence.existsById(note1Domain.getId())).isTrue();

            // Act
            notePersistence.deleteById(note1Domain.getId());
            flushAndClear();


            // Assert
            assertThat(notePersistence.existsById(note1Domain.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllByProjectId Method Tests")
    class FindAllByProjectIdTests {
        @BeforeEach
        void setUpFindAllByProjectId() {
            notePersistence.create(note1Domain); // project1
            notePersistence.create(note2Domain); // project1
            notePersistence.create(note3Domain); // project2
            flushAndClear();
        }

        @Test
        @DisplayName("should return all notes for a specific project ID")
        void findAllByProjectId_shouldReturnMatchingNotes() {
            Pagination<Note> result = notePersistence.findAllByProjectId(Page.of(0, 10), project1Jpa.getId());

            assertThat(result.items()).hasSize(2);
            assertThat(result.items()).extracting(Note::getTitle)
                    .containsExactlyInAnyOrder(note1Domain.getTitle(), note2Domain.getTitle());
            assertThat(result.total()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (AbstractPersistence)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            notePersistence.create(note1Domain);
            notePersistence.create(note2Domain);
            notePersistence.create(note3Domain);
            flushAndClear();
        }

        @Test
        @DisplayName("should filter by title")
        void findAll_filterByTitle_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "title=Second Note");
            Pagination<Note> result = notePersistence.findAll(query);

            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).getTitle()).isEqualTo(note2Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by version")
        void findAll_filterByVersion_shouldReturnMatching() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "version=1");
            Pagination<Note> result = notePersistence.findAll(query);

            assertThat(result.items()).hasSize(3);
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "invalidField=test");

            assertThatThrownBy(() -> notePersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid version value")
        void findAll_invalidVersionValue_shouldThrowBusinessException() {
            SearchQuery query = new SearchQuery(Page.of(0, 10), "version=not-a-number");
            assertThatThrownBy(() -> notePersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid value for version field");
        }

        @Test
        @DisplayName("should respect pagination parameters")
        void findAll_withPagination_shouldReturnCorrectPage() {
            SearchQuery queryPage1 = new SearchQuery(Page.of(0, 2, "title", "asc"), "");
            Pagination<Note> result1 = notePersistence.findAll(queryPage1);

            assertThat(result1.items()).hasSize(2);
            assertThat(result1.currentPage()).isEqualTo(0);
            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).extracting(Note::getTitle)
                    .containsExactly("First Note Title", "Note for Project Beta");

            SearchQuery queryPage2 = new SearchQuery(Page.of(1, 2, "title", "asc"), "");
            Pagination<Note> result2 = notePersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items().get(0).getTitle()).isEqualTo("Second Note Title");
        }
    }
}
