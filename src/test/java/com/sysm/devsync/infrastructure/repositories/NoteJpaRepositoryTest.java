package com.sysm.devsync.infrastructure.repositories;

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
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sysm.devsync.infrastructure.Utils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoteJpaRepositoryTest extends AbstractRepositoryTest {

    // Prerequisite entities
    private UserJpaEntity authorUser;
    private ProjectJpaEntity project1;

    // Entities under test
    private NoteJpaEntity note1;
    private NoteJpaEntity note2;
    private NoteJpaEntity note3;

    @BeforeEach
    void setUp() {
        // Use the inherited clear method
        clearRepositories();

        // 1. Create Users
        UserJpaEntity workspaceOwner = new UserJpaEntity(UUID.randomUUID().toString());
        workspaceOwner.setName("Workspace Owner");
        workspaceOwner.setEmail("ws.owner.note@example.com");
        workspaceOwner.setRole(UserRole.ADMIN);
        workspaceOwner.setCreatedAt(Instant.now());
        workspaceOwner.setUpdatedAt(Instant.now());
        entityPersist(workspaceOwner);

        authorUser = new UserJpaEntity(UUID.randomUUID().toString());
        authorUser.setName("Note Author");
        authorUser.setEmail("note.author@example.com");
        authorUser.setRole(UserRole.MEMBER);
        authorUser.setCreatedAt(Instant.now());
        authorUser.setUpdatedAt(Instant.now());
        entityPersist(authorUser);

        // 2. Create Workspace
        WorkspaceJpaEntity workspace = new WorkspaceJpaEntity(UUID.randomUUID().toString());
        workspace.setName("Workspace for Note Tests");
        workspace.setOwner(workspaceOwner);
        workspace.setCreatedAt(Instant.now());
        workspace.setUpdatedAt(Instant.now());
        entityPersist(workspace);

        // 3. Create Projects
        project1 = new ProjectJpaEntity(UUID.randomUUID().toString());
        project1.setName("Project Alpha for Notes");
        project1.setWorkspace(workspace);
        project1.setCreatedAt(Instant.now());
        project1.setUpdatedAt(Instant.now());
        entityPersist(project1);

        ProjectJpaEntity project2 = new ProjectJpaEntity(UUID.randomUUID().toString());
        project2.setName("Project Beta for Notes");
        project2.setWorkspace(workspace);
        project2.setCreatedAt(Instant.now());
        project2.setUpdatedAt(Instant.now());
        entityManager.persist(project2);

        // 4. Create Tags
        TagJpaEntity tagJava = new TagJpaEntity(UUID.randomUUID().toString(), "java");
        entityManager.persist(tagJava);
        TagJpaEntity tagSpring = new TagJpaEntity(UUID.randomUUID().toString(), "spring");
        entityManager.persist(tagSpring);
        TagJpaEntity tagJpa = new TagJpaEntity(UUID.randomUUID().toString(), "jpa");
        entityManager.persist(tagJpa);

        flushAndClear();

        // 5. Create Note Entities (in memory, to be used in tests)
        note1 = new NoteJpaEntity(UUID.randomUUID().toString());
        note1.setTitle("First Note Title");
        note1.setContent("This is the content for the first note, discussing Java.");
        note1.setVersion(1);
        note1.setAuthor(authorUser);
        note1.setProject(project1);
        note1.setTags(Set.of(tagJava, tagSpring));
        note1.setCreatedAt(Instant.now());
        note1.setUpdatedAt(Instant.now());

        note2 = new NoteJpaEntity(UUID.randomUUID().toString());
        note2.setTitle("Second Note Title");
        note2.setContent("Content for the second note, about Spring and JPA.");
        note2.setVersion(1);
        note2.setAuthor(authorUser);
        note2.setProject(project1);
        note2.setTags(Set.of(tagSpring, tagJpa));
        note2.setCreatedAt(Instant.now());
        note2.setUpdatedAt(Instant.now());

        note3 = new NoteJpaEntity(UUID.randomUUID().toString());
        note3.setTitle("Note for Project Beta");
        note3.setContent("This note belongs to project Beta and is about Java.");
        note3.setVersion(1);
        note3.setAuthor(authorUser);
        note3.setProject(project2);
        note3.setTags(Set.of(tagJava));
        note3.setCreatedAt(Instant.now());
        note3.setUpdatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Save and Find Tests")
    class SaveAndFindTests {
        @Test
        @DisplayName("should save a note and find it by id")
        void saveAndFindById() {
            // Act
            NoteJpaEntity savedNote = noteJpaRepository.save(note1);
            flushAndClear();

            Optional<NoteJpaEntity> foundNoteOpt = noteJpaRepository.findById(savedNote.getId());

            // Assert
            assertThat(foundNoteOpt).isPresent();
            NoteJpaEntity foundNote = foundNoteOpt.get();
            assertThat(foundNote.getTitle()).isEqualTo(note1.getTitle());
            assertThat(foundNote.getContent()).isEqualTo(note1.getContent());
            assertThat(foundNote.getAuthor().getId()).isEqualTo(authorUser.getId());
            assertThat(foundNote.getProject().getId()).isEqualTo(project1.getId());
            assertThat(foundNote.getTags()).hasSize(2);
            assertThat(foundNote.getCreatedAt()).isNotNull();
            assertThat(foundNote.getUpdatedAt()).isNotNull();
            assertThat(foundNote.getVersion()).isOne(); // Initial version should be 1
        }

        @Test
        @DisplayName("should fail to save note with null title")
        void save_withNullTitle_shouldFail() {
            // Arrange
            note1.setTitle(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                noteJpaRepository.save(note1);
                flushAndClear();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("should fail to save note with null author")
        void save_withNullAuthor_shouldFail() {
            // Arrange
            note1.setAuthor(null);

            // Act & Assert
            assertThatThrownBy(() -> {
                noteJpaRepository.save(note1);
                flushAndClear();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("Update and Delete Tests")
    class UpdateAndDeleteTests {
        @Test
        @DisplayName("should update an existing note")
        void updateNote() {
            // Arrange
            NoteJpaEntity persistedNote = noteJpaRepository.save(note1);
            flushAndClear();
            Instant originalUpdatedAt = persistedNote.getUpdatedAt();
            Integer originalVersion = persistedNote.getVersion();

            sleep(100);

            // Act
            persistedNote.setTitle("Updated Note Title");
            persistedNote.setContent("Updated content for the first note.");
            persistedNote.setUpdatedAt(Instant.now());
            persistedNote.setVersion(originalVersion + 1); // Increment version
            noteJpaRepository.save(persistedNote);
            flushAndClear();


            // Assert
            Optional<NoteJpaEntity> updatedNoteOpt = noteJpaRepository.findById(persistedNote.getId());
            assertThat(updatedNoteOpt).isPresent();
            NoteJpaEntity updatedNote = updatedNoteOpt.get();
            assertThat(updatedNote.getTitle()).isEqualTo("Updated Note Title");
            assertThat(updatedNote.getUpdatedAt()).isAfter(originalUpdatedAt);
            assertThat(updatedNote.getVersion()).isEqualTo(originalVersion + 1);
        }

        @Test
        @DisplayName("should delete a note by id")
        void deleteById() {
            // Arrange
            NoteJpaEntity persistedNote = noteJpaRepository.save(note1);
            flushAndClear();
            String idToDelete = persistedNote.getId();

            // Act
            noteJpaRepository.deleteById(idToDelete);
            flushAndClear();


            // Assert
            Optional<NoteJpaEntity> deletedNote = noteJpaRepository.findById(idToDelete);
            assertThat(deletedNote).isNotPresent();
        }
    }

    @Nested
    @DisplayName("Custom Query Tests")
    class CustomQueryTests {
        @Test
        @DisplayName("findAllByProject_Id should return all notes for a specific project")
        void findAllByProject_Id_shouldReturnMatchingNotes() {
            // Arrange
            noteJpaRepository.save(note1); // For project1
            noteJpaRepository.save(note2); // For project1
            noteJpaRepository.save(note3); // For project2
            flushAndClear();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<NoteJpaEntity> project1Notes = noteJpaRepository.findAllByProject_Id(project1.getId(), pageable);

            // Assert
            assertThat(project1Notes.getTotalElements()).isEqualTo(2);
            assertThat(project1Notes.getContent()).extracting(NoteJpaEntity::getTitle)
                    .containsExactlyInAnyOrder(note1.getTitle(), note2.getTitle());
        }

        @Test
        @DisplayName("findAllByProject_Id should return an empty page for a project with no notes")
        void findAllByProject_Id_noMatches_shouldReturnEmptyPage() {
            // Arrange
            noteJpaRepository.save(note3); // Only save note for project2
            flushAndClear();

            // Act
            Pageable pageable = PageRequest.of(0, 10);
            Page<NoteJpaEntity> project1Notes = noteJpaRepository.findAllByProject_Id(project1.getId(), pageable);

            // Assert
            assertThat(project1Notes.getTotalElements()).isZero();
            assertThat(project1Notes.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Specification Tests")
    class SpecificationTests {
        @BeforeEach
        void setUpSpecs() {
            noteJpaRepository.save(note1); // Title: First, Content: Java
            noteJpaRepository.save(note2); // Title: Second, Content: Spring
            noteJpaRepository.save(note3); // Title: Note, Content: Java
            flushAndClear();
        }

        @Test
        @DisplayName("should filter notes by title")
        void findAll_withSpecification_byTitle() {
            // Arrange
            Specification<NoteJpaEntity> spec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%second%");
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<NoteJpaEntity> result = noteJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(note2.getId());
        }

        @Test
        @DisplayName("should filter notes by content")
        void findAll_withSpecification_byContent() {
            // Arrange
            Specification<NoteJpaEntity> spec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("content")), "%spring and jpa%");
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<NoteJpaEntity> result = noteJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(note2.getId());
        }

        @Test
        @DisplayName("should filter notes by tag name (requires join)")
        void findAll_withSpecification_byTagName() {
            // Arrange
            Specification<NoteJpaEntity> spec = (root, query, cb) -> {
                query.distinct(true); // Use distinct to avoid duplicates if a note has multiple matching tags
                return cb.equal(root.join("tags").get("name"), "java");
            };
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<NoteJpaEntity> result = noteJpaRepository.findAll(spec, pageable);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).extracting(NoteJpaEntity::getTitle)
                    .containsExactlyInAnyOrder(note1.getTitle(), note3.getTitle());
        }
    }
}
