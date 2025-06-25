package com.sysm.devsync.application;

import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.NoteCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Note;
import com.sysm.devsync.domain.persistence.NotePersistencePort;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NotePersistencePort notePersistence;
    @Mock
    private ProjectPersistencePort projectPersistence;
    @Mock
    private UserPersistencePort userPersistence;
    @Mock
    private TagPersistencePort tagPersistence;

    @InjectMocks
    private NoteService noteService;

    private String noteId;
    private String projectId;
    private String authorId;
    private String tagId;
    private NoteCreateUpdate noteCreateUpdateDto;
    private Note mockNote;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();
        authorId = UUID.randomUUID().toString();
        tagId = UUID.randomUUID().toString();

        noteCreateUpdateDto = new NoteCreateUpdate(
                "Test Note Title",
                "Test note content.",
                projectId
        );
        mockNote = mock(Note.class); // Used for methods that find and then operate on a note
    }

    @Test
    @DisplayName("createNote should create and save note when project and user exist")
    void createNote_shouldCreateAndSaveNote_whenProjectAndUserExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(userPersistence.existsById(authorId)).thenReturn(true);
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);

        // Act
        CreateResponse response = noteService.createNote(noteCreateUpdateDto, authorId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.id());
        verify(projectPersistence).existsById(projectId);
        verify(userPersistence).existsById(authorId);
        verify(notePersistence).create(noteCaptor.capture());

        Note capturedNote = noteCaptor.getValue();
        assertEquals(noteCreateUpdateDto.title(), capturedNote.getTitle()); // Assuming Note.create sets 'name' for title
        assertEquals(noteCreateUpdateDto.content(), capturedNote.getContent());
        assertEquals(projectId, capturedNote.getProjectId());
        assertEquals(authorId, capturedNote.getAuthorId());
        assertEquals(response.id(), capturedNote.getId());
    }

    @Test
    @DisplayName("createNote should throw IllegalArgumentException when project does not exist")
    void createNote_shouldThrowException_whenProjectDoesNotExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.createNote(noteCreateUpdateDto, authorId);
        });
        assertEquals("Project not found", exception.getMessage());
        verify(userPersistence, never()).existsById(anyString());
        verify(notePersistence, never()).create(any(Note.class));
    }

    @Test
    @DisplayName("createNote should throw IllegalArgumentException when user does not exist")
    void createNote_shouldThrowException_whenUserDoesNotExist() {
        // Arrange
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(userPersistence.existsById(authorId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.createNote(noteCreateUpdateDto, authorId);
        });
        assertEquals("User not found", exception.getMessage());
        verify(notePersistence, never()).create(any(Note.class));
    }

    @Test
    @DisplayName("updateNote should update existing note")
    void updateNote_shouldUpdateExistingNote() {
        // Arrange
        NoteCreateUpdate updateDto = new NoteCreateUpdate("Updated Title", "Updated Content", projectId);
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));

        // Act
        noteService.updateNote(noteId, updateDto);

        // Assert
        verify(notePersistence).findById(noteId);
        verify(mockNote).update(updateDto.title(), updateDto.content());
        verify(notePersistence).update(mockNote);
    }

    @Test
    @DisplayName("updateNote should throw IllegalArgumentException when note not found")
    void updateNote_shouldThrowException_whenNoteNotFound() {
        // Arrange
        NoteCreateUpdate updateDto = new NoteCreateUpdate("Updated Title", "Updated Content", projectId);
        when(notePersistence.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.updateNote(noteId, updateDto);
        });
        assertEquals("Note not found", exception.getMessage());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("updateNoteContent should update existing note's content")
    void updateNoteContent_shouldUpdateExistingNoteContent() {
        // Arrange
        NoteCreateUpdate updateDto = new NoteCreateUpdate(null, "Only Content Updated", null); // Title and projectId are not used
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));

        // Act
        noteService.updateNoteContent(noteId, updateDto);

        // Assert
        verify(notePersistence).findById(noteId);
        verify(mockNote).updateContent(updateDto.content());
        verify(notePersistence).update(mockNote);
    }

    @Test
    @DisplayName("updateNoteContent should throw IllegalArgumentException when note not found")
    void updateNoteContent_shouldThrowException_whenNoteNotFound() {
        // Arrange
        NoteCreateUpdate updateDto = new NoteCreateUpdate(null, "Content", null);
        when(notePersistence.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.updateNoteContent(noteId, updateDto);
        });
        assertEquals("Note not found", exception.getMessage());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("addTagToNote should add tag and update note when note and tag exist")
    void addTagToNote_shouldAddTagAndUpdateNote_whenAllExist() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));
        when(tagPersistence.existsById(tagId)).thenReturn(true);

        // Act
        noteService.addTagToNote(noteId, tagId);

        // Assert
        verify(notePersistence).findById(noteId);
        verify(tagPersistence).existsById(tagId);
        verify(mockNote).addTag(tagId);
        verify(notePersistence).update(mockNote);
    }

    @Test
    @DisplayName("addTagToNote should throw IllegalArgumentException when note not found")
    void addTagToNote_shouldThrowException_whenNoteNotFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.addTagToNote(noteId, tagId);
        });
        assertEquals("Note not found", exception.getMessage());
        verify(tagPersistence, never()).existsById(anyString());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("addTagToNote should throw IllegalArgumentException when tag not found")
    void addTagToNote_shouldThrowException_whenTagNotFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));
        when(tagPersistence.existsById(tagId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.addTagToNote(noteId, tagId);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(mockNote, never()).addTag(anyString());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("removeTagFromNote should remove tag and update note when note and tag exist")
    void removeTagFromNote_shouldRemoveTagAndUpdateNote_whenAllExist() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));
        when(tagPersistence.existsById(tagId)).thenReturn(true);

        // Act
        noteService.removeTagFromNote(noteId, tagId);

        // Assert
        verify(notePersistence).findById(noteId);
        verify(tagPersistence).existsById(tagId);
        verify(mockNote).removeTag(tagId);
        verify(notePersistence).update(mockNote);
    }

    @Test
    @DisplayName("removeTagFromNote should throw IllegalArgumentException when note not found")
    void removeTagFromNote_shouldThrowException_whenNoteNotFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.removeTagFromNote(noteId, tagId);
        });
        assertEquals("Note not found", exception.getMessage());
        verify(tagPersistence, never()).existsById(anyString());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("removeTagFromNote should throw IllegalArgumentException when tag not found")
    void removeTagFromNote_shouldThrowException_whenTagNotFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));
        when(tagPersistence.existsById(tagId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.removeTagFromNote(noteId, tagId);
        });
        assertEquals("Tag not found", exception.getMessage());
        verify(mockNote, never()).removeTag(anyString());
        verify(notePersistence, never()).update(any(Note.class));
    }

    @Test
    @DisplayName("deleteNote should call persistence deleteById when note exists")
    void deleteNote_shouldCallPersistenceDeleteById_whenNoteExists() {
        // Arrange
        when(notePersistence.existsById(noteId)).thenReturn(true);
        doNothing().when(notePersistence).deleteById(noteId);

        // Act
        noteService.deleteNote(noteId);

        // Assert
        verify(notePersistence).existsById(noteId);
        verify(notePersistence).deleteById(noteId);
    }

    @Test
    @DisplayName("deleteNote should throw IllegalArgumentException when note not found")
    void deleteNote_shouldThrowException_whenNoteNotFound() {
        // Arrange
        when(notePersistence.existsById(noteId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.deleteNote(noteId);
        });
        assertEquals("Note not found", exception.getMessage());
        verify(notePersistence, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("getNoteById should return note when found")
    void getNoteById_shouldReturnNote_whenFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.of(mockNote));

        // Act
        Note actualNote = noteService.getNoteById(noteId);

        // Assert
        assertNotNull(actualNote);
        assertSame(mockNote, actualNote);
        verify(notePersistence).findById(noteId);
    }

    @Test
    @DisplayName("getNoteById should throw IllegalArgumentException when note not found")
    void getNoteById_shouldThrowException_whenNoteNotFound() {
        // Arrange
        when(notePersistence.findById(noteId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.getNoteById(noteId);
        });
        assertEquals("Note not found", exception.getMessage());
    }

    @Test
    @DisplayName("getAllNotes with SearchQuery should return page from persistence")
    void getAllNotes_withSearchQuery_shouldReturnPageFromPersistence() {
        // Arrange
        SearchQuery query = new SearchQuery(new Page(0, 10, "createdAt", "DESC"),"search term");
        Pagination<Note> expectedPagination = new Pagination<>(0, 10, 0L, Collections.emptyList());
        when(notePersistence.findAll(query)).thenReturn(expectedPagination);

        // Act
        Pagination<Note> actualPagination = noteService.getAllNotes(query);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(notePersistence).findAll(query);
    }

    @Test
    @DisplayName("getAllNotes with SearchQuery should throw IllegalArgumentException when query is null")
    void getAllNotes_withSearchQuery_shouldThrowException_whenQueryIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            noteService.getAllNotes(null);
        });
        assertEquals("Invalid query parameters", exception.getMessage());
        verify(notePersistence, never()).findAll(any(SearchQuery.class));
    }

    @Test
    @DisplayName("getAllNotes with Pageable and projectId should return page when project exists")
    void getAllNotes_withPageableAndProjectId_shouldReturnPage_whenProjectExists() {
        // Arrange
        Page page =  new Page(0, 10, "createdAt", "asc");
        Pagination<Note> expectedPagination = new Pagination<>(0, 10, 0L, Collections.emptyList());
        when(projectPersistence.existsById(projectId)).thenReturn(true);
        when(notePersistence.findAllByProjectId(page, projectId)).thenReturn(expectedPagination);

        // Act
        Pagination<Note> actualPagination = noteService.getAllNotes(page, projectId);

        // Assert
        assertNotNull(actualPagination);
        assertSame(expectedPagination, actualPagination);
        verify(projectPersistence).existsById(projectId);
        verify(notePersistence).findAllByProjectId(page, projectId);
    }

    @Test
    @DisplayName("getAllNotes with Pageable and projectId should throw IllegalArgumentException when project not found")
    void getAllNotes_withPageableAndProjectId_shouldThrowException_whenProjectNotFound() {
        // Arrange
        Page page = new Page(0, 10, "createdAt", "asc");
        when(projectPersistence.existsById(projectId)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            noteService.getAllNotes(page, projectId);
        });
        assertEquals("Project not found", exception.getMessage());
        verify(notePersistence, never()).findAllByProjectId(any(Page.class), anyString());
    }
}
