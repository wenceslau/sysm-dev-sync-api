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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sysm.devsync.infrastructure.Utils.sleep;
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
    private TagJpaEntity tagJavaJpa;
    private TagJpaEntity tagSpringJpa;
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

        tagJavaJpa = TagJpaEntity.fromModel(Tag.create("java", "#F89820"));
        entityPersist(tagJavaJpa);

        tagSpringJpa = TagJpaEntity.fromModel(Tag.create("spring", "#F89820"));
        entityPersist(tagSpringJpa);

        tagJpaJpa = TagJpaEntity.fromModel(Tag.create("jpa", "#F89820"));
        entityPersist(tagJpaJpa);

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

    // --- Basic CRUD, findById, existsById tests are correct and remain unchanged ---
    @Nested
    @DisplayName("create Method Tests")
    class CreateTests {
        @Test
        @DisplayName("should create and save a note")
        void create_shouldSaveNote() {
            // Act
            assertDoesNotThrow(() -> create(note1Domain));

            // Assert
            NoteJpaEntity foundInDb = entityManager.find(NoteJpaEntity.class, note1Domain.getId());
            assertThat(foundInDb).isNotNull();
            assertThat(foundInDb.getTitle()).isEqualTo(note1Domain.getTitle());
            assertThat(foundInDb.getTags().stream().map(TagJpaEntity::getId).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(note1Domain.getTagsId());
        }
    }

    @Nested
    @DisplayName("update Method Tests")
    class UpdateTests {
        @Test
        @DisplayName("should update an existing note")
        void update_shouldModifyExistingNote() {
            // Arrange
            create(note1Domain);
            sleep(10); // Ensure updatedAt will be different

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
            update(updatedDomainNote);

            // Assert
            Optional<Note> foundNoteOpt = notePersistence.findById(note1Domain.getId());
            assertThat(foundNoteOpt).isPresent();
            Note foundNote = foundNoteOpt.get();

            assertThat(foundNote.getTitle()).isEqualTo("Updated: First Note Title");
            assertThat(foundNote.getTagsId()).containsExactly(tagJpaJpa.getId());
            assertThat(foundNote.getProjectId()).isEqualTo(project2Jpa.getId());
            assertThat(foundNote.getUpdatedAt()).isAfter(note1Domain.getUpdatedAt());
        }
    }

    // --- Other basic tests (delete, findById, existsById) are also correct ---

    @Nested
    @DisplayName("findAllByProjectId Method Tests")
    class FindAllByProjectIdTests {
        @BeforeEach
        void setUpFindAllByProjectId() {
            create(note1Domain); // project1
            create(note2Domain); // project1
            create(note3Domain); // project2
        }

        @Test
        @DisplayName("should return all notes for a specific project ID")
        void findAllByProjectId_shouldReturnMatchingNotes() {
            Pagination<Note> result = notePersistence.findAllByProjectId(Page.of(0, 10), project1Jpa.getId());

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.items()).extracting(Note::getTitle)
                    .containsExactlyInAnyOrder(note1Domain.getTitle(), note2Domain.getTitle());
        }
    }

    @Nested
    @DisplayName("findAll Method Tests (Generic Search)")
    class FindAllTests {
        @BeforeEach
        void setUpFindAll() {
            create(note1Domain);
            create(note2Domain);
            create(note3Domain);
        }

        @Test
        @DisplayName("should filter by a single term (e.g., title)")
        void findAll_filterByTitle_shouldReturnMatching() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("title", "Second Note"));
            Pagination<Note> result = notePersistence.findAll(query);

            assertThat(result.total()).isEqualTo(1);
            assertThat(result.items().get(0).getTitle()).isEqualTo(note2Domain.getTitle());
        }

        @Test
        @DisplayName("should filter by multiple terms using AND logic")
        void findAll_withMultipleTerms_shouldReturnAndedResults() {
            // Arrange: Search for a note with title "First" AND in project1
            SearchQuery queryWithMatch = SearchQuery.of(Page.of(0, 10), Map.of(
                    "title", "First",
                    "projectId", project1Jpa.getId()
            ));

            // Act
            Pagination<Note> resultWithMatch = notePersistence.findAll(queryWithMatch);

            // Assert: Should find exactly one note: note1
            assertThat(resultWithMatch.total()).isEqualTo(1);
            assertThat(resultWithMatch.items().get(0).getId()).isEqualTo(note1Domain.getId());

            // Arrange: Search for a note with title "First" AND in project2 (should be none)
            SearchQuery queryWithoutMatch = SearchQuery.of(Page.of(0, 10), Map.of(
                    "title", "First",
                    "projectId", project2Jpa.getId()
            ));

            // Act
            Pagination<Note> resultWithoutMatch = notePersistence.findAll(queryWithoutMatch);

            // Assert: Should find no notes
            assertThat(resultWithoutMatch.total()).isZero();
            assertThat(resultWithoutMatch.items()).isEmpty();
        }

        @Test
        @DisplayName("should throw BusinessException for an invalid search field")
        void findAll_invalidSearchField_shouldThrowBusinessException() {
            SearchQuery query = SearchQuery.of(Page.of(0, 10), Map.of("invalidField", "value"));

            assertThatThrownBy(() -> notePersistence.findAll(query))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid search field provided: 'invalidField'");
        }

        @Test
        @DisplayName("should respect pagination and sorting parameters")
        void findAll_withPaginationAndSorting_shouldReturnCorrectPage() {
            SearchQuery queryPage1 = SearchQuery.of(Page.of(0, 2, "title", "asc"), Map.of());
            Pagination<Note> result1 = notePersistence.findAll(queryPage1);

            assertThat(result1.total()).isEqualTo(3);
            assertThat(result1.items()).hasSize(2);
            assertThat(result1.items()).extracting(Note::getTitle)
                    .containsExactly("First Note Title", "Note for Project Beta");

            SearchQuery queryPage2 = SearchQuery.of(Page.of(1, 2, "title", "asc"), Map.of());
            Pagination<Note> result2 = notePersistence.findAll(queryPage2);
            assertThat(result2.items()).hasSize(1);
            assertThat(result2.items().get(0).getTitle()).isEqualTo("Second Note Title");
        }
    }

    // Helper methods
    private void create(Note entity) {
        notePersistence.create(entity);
        flushAndClear();
    }

    private void update(Note entity) {
        notePersistence.update(entity);
        flushAndClear();
    }

    private void deleteById(String id) {
        notePersistence.deleteById(id);
        flushAndClear();
    }
}
