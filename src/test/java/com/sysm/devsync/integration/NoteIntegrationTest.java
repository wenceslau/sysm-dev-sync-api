package com.sysm.devsync.integration;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.infrastructure.controllers.dto.request.NoteCreateUpdate;
import com.sysm.devsync.infrastructure.repositories.*;
import com.sysm.devsync.infrastructure.repositories.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class NoteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NoteJpaRepository noteJpaRepository;
    @Autowired
    private ProjectJpaRepository projectJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private TagJpaRepository tagJpaRepository;
    @Autowired
    private WorkspaceJpaRepository workspaceJpaRepository;

    // This ID is hardcoded in the controller for the "authenticated" user
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    private UserJpaEntity testAuthor1;
    private UserJpaEntity testAuthor2;
    private ProjectJpaEntity testProject1;
    private ProjectJpaEntity testProject2;
    private TagJpaEntity testTag;

    @BeforeEach
    void setUp() {
        // Clean all relevant repositories to ensure test isolation
        noteJpaRepository.deleteAll();
        tagJpaRepository.deleteAll();
        projectJpaRepository.deleteAll();
        workspaceJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        // 1. Create users
        testAuthor1 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author One", "author1@test.com", UserRole.MEMBER)));
        testAuthor2 = userJpaRepository.saveAndFlush(UserJpaEntity.fromModel(User.create("Author Two", "author2@test.com", UserRole.MEMBER)));

        // 2. Ensure the fake authenticated user (who will create the note) exists
        if (!userJpaRepository.existsById(FAKE_AUTHENTICATED_USER_ID)) {
            UserJpaEntity fakeAuthUser = new UserJpaEntity();
            fakeAuthUser.setId(FAKE_AUTHENTICATED_USER_ID);
            fakeAuthUser.setName("Controller User");
            fakeAuthUser.setEmail("controller.user@example.com");
            fakeAuthUser.setRole(UserRole.ADMIN);
            fakeAuthUser.setCreatedAt(Instant.now());
            fakeAuthUser.setUpdatedAt(Instant.now());
            userJpaRepository.saveAndFlush(fakeAuthUser);
        }

        // 3. Create a workspace and projects
        var testWorkspace = workspaceJpaRepository.saveAndFlush(WorkspaceJpaEntity.fromModel(Workspace.create("Test WS", "Desc", true, testAuthor1.getId())));
        testProject1 = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project 1", "Desc", testWorkspace.getId())));
        testProject2 = projectJpaRepository.saveAndFlush(ProjectJpaEntity.fromModel(Project.create("Test Project 2", "Desc", testWorkspace.getId())));

        // 4. Create a tag to be used in tests
        testTag = tagJpaRepository.saveAndFlush(TagJpaEntity.fromModel(Tag.create("Test Tag", "A tag for notes")));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /notes - should create a new note successfully")
    void createNote_shouldSucceed() throws Exception {
        // Arrange
        var requestDto = new NoteCreateUpdate("My First Note", "This is the content of the note.", testProject1.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(post("/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(header().exists("Location"));

        // Verify DB state
        var createdNote = noteJpaRepository.findAll().get(0);
        assertThat(createdNote.getTitle()).isEqualTo("My First Note");
        assertThat(createdNote.getProject().getId()).isEqualTo(testProject1.getId());
        assertThat(createdNote.getAuthor().getId()).isEqualTo(FAKE_AUTHENTICATED_USER_ID);
        assertThat(createdNote.getVersion()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PUT /notes/{id} - should update a note's title and content")
    void updateNote_shouldSucceed() throws Exception {
        // Arrange
        var noteModel = com.sysm.devsync.domain.models.Note.create("Old Title", "Old Content", testProject1.getId(), testAuthor1.getId());
        var savedNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));

        var requestDto = new NoteCreateUpdate("New Title", "New Content", testProject1.getId());
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(put("/notes/{id}", savedNote.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<NoteJpaEntity> updatedNote = noteJpaRepository.findById(savedNote.getId());
        assertThat(updatedNote).isPresent();
        assertThat(updatedNote.get().getTitle()).isEqualTo("New Title");
        assertThat(updatedNote.get().getContent()).isEqualTo("New Content");
        assertThat(updatedNote.get().getVersion()).isEqualTo(2); // Version should increment
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("PATCH /notes/{id}/content - should partially update a note's content")
    void updateNoteContent_shouldSucceed() throws Exception {
        // Arrange
        var noteModel = com.sysm.devsync.domain.models.Note.create("Title", "Original Content", testProject1.getId(), testAuthor1.getId());
        var savedNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));

        // Using NoteCreateUpdate as per your decision, but only 'content' is relevant for this endpoint
        var requestDto = new NoteCreateUpdate("Ignored Title", "Updated Partial Content", "ignored-project-id");
        var requestJson = objectMapper.writeValueAsString(requestDto);

        // Act & Assert
        mockMvc.perform(patch("/notes/{id}/content", savedNote.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<NoteJpaEntity> updatedNote = noteJpaRepository.findById(savedNote.getId());
        assertThat(updatedNote).isPresent();
        assertThat(updatedNote.get().getTitle()).isEqualTo("Title"); // Title should remain unchanged
        assertThat(updatedNote.get().getContent()).isEqualTo("Updated Partial Content");
        assertThat(updatedNote.get().getVersion()).isEqualTo(2); // Version should increment
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN"})
    @DisplayName("DELETE /notes/{id} - should delete an existing note")
    void deleteNote_shouldSucceed() throws Exception {
        // Arrange
        var noteModel = com.sysm.devsync.domain.models.Note.create("To Be Deleted", "Content", testProject1.getId(), testAuthor1.getId());
        var savedNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));
        assertThat(noteJpaRepository.existsById(savedNote.getId())).isTrue();

        // Act & Assert
        mockMvc.perform(delete("/notes/{id}", savedNote.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        assertThat(noteJpaRepository.existsById(savedNote.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("POST /notes/{id}/tags/{tagId} - should add a tag to a note")
    void addTagToNote_shouldSucceed() throws Exception {
        // Arrange
        var noteModel = com.sysm.devsync.domain.models.Note.create("Note with Tags", "Content", testProject1.getId(), testAuthor1.getId());
        var savedNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));
        assertThat(savedNote.getTags()).isEmpty();

        // Act & Assert
        mockMvc.perform(post("/notes/{id}/tags/{tagId}", savedNote.getId(), testTag.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<NoteJpaEntity> updatedNote = noteJpaRepository.findById(savedNote.getId());
        assertThat(updatedNote).isPresent();
        assertThat(updatedNote.get().getTags()).hasSize(1);
        assertThat(updatedNote.get().getTags().iterator().next().getId()).isEqualTo(testTag.getId());
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("DELETE /notes/{id}/tags/{tagId} - should remove a tag from a note")
    void removeTagFromNote_shouldSucceed() throws Exception {
        // Arrange
        var noteModel = com.sysm.devsync.domain.models.Note.create("Note to Untag", "Content", testProject1.getId(), testAuthor1.getId());
        var savedNote = noteJpaRepository.saveAndFlush(NoteJpaEntity.fromModel(noteModel));
        savedNote.getTags().add(testTag); // Add tag directly for setup
        noteJpaRepository.saveAndFlush(savedNote); // Persist the tag relationship
        assertThat(savedNote.getTags()).hasSize(1);

        // Act & Assert
        mockMvc.perform(delete("/notes/{id}/tags/{tagId}", savedNote.getId(), testTag.getId()))
                .andExpect(status().isNoContent());

        // Verify DB state
        Optional<NoteJpaEntity> updatedNote = noteJpaRepository.findById(savedNote.getId());
        assertThat(updatedNote).isPresent();
        assertThat(updatedNote.get().getTags()).isEmpty();
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /notes - should return paginated list of notes")
    void searchNotes_shouldReturnPaginatedResults() throws Exception {
        // Arrange
        noteJpaRepository.save(NoteJpaEntity.fromModel(com.sysm.devsync.domain.models.Note.create("Note C", "...", testProject1.getId(), testAuthor1.getId())));
        noteJpaRepository.save(NoteJpaEntity.fromModel(com.sysm.devsync.domain.models.Note.create("Note A", "...", testProject1.getId(), testAuthor1.getId())));
        noteJpaRepository.save(NoteJpaEntity.fromModel(com.sysm.devsync.domain.models.Note.create("Note B", "...", testProject1.getId(), testAuthor1.getId())));
        noteJpaRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/notes")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .param("sort", "title")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Note A"))
                .andExpect(jsonPath("$.items[1].title").value("Note B"));
    }

    @Test
    @WithMockUser(username = FAKE_AUTHENTICATED_USER_ID, roles = {"ADMIN", "MEMBER"})
    @DisplayName("GET /notes - should return notes filtered by query parameters")
    void searchNotes_withFilters_shouldReturnFilteredResults() throws Exception {
        // Arrange
        var note1 = com.sysm.devsync.domain.models.Note.create("Java Note", "Content about Java", testProject1.getId(), testAuthor1.getId());
        noteJpaRepository.save(NoteJpaEntity.fromModel(note1));

        var note2 = com.sysm.devsync.domain.models.Note.create("Spring Note", "Content about Spring", testProject1.getId(), testAuthor2.getId());
        noteJpaRepository.save(NoteJpaEntity.fromModel(note2));

        var note3 = com.sysm.devsync.domain.models.Note.create("Another Java Note", "More content about Java", testProject2.getId(), testAuthor1.getId());
        noteJpaRepository.save(NoteJpaEntity.fromModel(note3));
        noteJpaRepository.flush();

        // Act & Assert - Filter by title
        mockMvc.perform(get("/notes").param("title", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(note2.getId()));

        // Act & Assert - Filter by projectId
        mockMvc.perform(get("/notes").param("projectId", testProject2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(note3.getId()));

        // Act & Assert - Filter by authorId
        mockMvc.perform(get("/notes").param("authorId", testAuthor2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(note2.getId()));

        // Act & Assert - Filter by multiple fields (authorId and projectId)
        mockMvc.perform(get("/notes")
                        .param("authorId", testAuthor1.getId())
                        .param("projectId", testProject1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(note1.getId()));

        // Act & Assert - Filter with no results
        mockMvc.perform(get("/notes")
                        .param("title", "Java")
                        .param("authorId", testAuthor2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
