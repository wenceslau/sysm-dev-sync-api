package com.sysm.devsync.application;

import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.infrastructure.controller.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.NoteCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Note;
import com.sysm.devsync.domain.persistence.NotePersistencePort;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.domain.persistence.UserPersistencePort;

public class NoteService {

    private final NotePersistencePort notePersistence;
    private final ProjectPersistencePort projectPersistence;
    private final UserPersistencePort userPersistence;
    private final TagPersistencePort tagPersistence;

    public NoteService(NotePersistencePort notePersistence, ProjectPersistencePort projectPersistence,
                       UserPersistencePort userPersistence, TagPersistencePort tagPersistence) {
        this.notePersistence = notePersistence;
        this.projectPersistence = projectPersistence;
        this.userPersistence = userPersistence;
        this.tagPersistence = tagPersistence;
    }

    public CreateResponse createNote(NoteCreateUpdate noteCreateUpdate, String authorId) {

        var exist = projectPersistence.existsById(noteCreateUpdate.projectId());
        if (!exist) {
            throw new NotFoundException("Project not found", noteCreateUpdate.projectId());
        }

        var userExists = userPersistence.existsById(authorId);
        if (!userExists) {
            throw new NotFoundException("User not found", authorId);
        }

        var note = Note.create(
                noteCreateUpdate.title(),
                noteCreateUpdate.content(),
                noteCreateUpdate.projectId(),
                authorId
        );

        notePersistence.create(note);
        return new CreateResponse(note.getId());
    }

    public void updateNote(String noteId, NoteCreateUpdate noteUpdate) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found", noteId));

        note.update(
                noteUpdate.title(),
                noteUpdate.content()
        );

        notePersistence.update(note);
    }

    public void updateNoteContent(String noteId, NoteCreateUpdate noteUpdate) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found", noteId));

        note.updateContent(noteUpdate.content());

        notePersistence.update(note);
    }

    public void addTagToNote(String noteId, String tagId) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found", noteId));

        var exists = tagPersistence.existsById(tagId);
        if (!exists) {
            throw new NotFoundException("Tag not found", tagId);
        }

        note.addTag(tagId);
        notePersistence.update(note);
    }

    public void removeTagFromNote(String noteId, String tagId) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found", noteId));

        var exists = tagPersistence.existsById(tagId);
        if (!exists) {
            throw new NotFoundException("Tag not found", tagId);
        }

        note.removeTag(tagId);
        notePersistence.update(note);
    }

    public void deleteNote(String noteId) {
        var exists = notePersistence.existsById(noteId);

        if (!exists) {
            throw new NotFoundException("Note not found", noteId);
        }

        notePersistence.deleteById(noteId);
    }

    public Note getNoteById(String noteId) {
        return notePersistence.findById(noteId)
                .orElseThrow(() -> new NotFoundException("Note not found", noteId));
    }

    public Pagination<Note> getAllNotes(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return notePersistence.findAll(query);
    }

    public Pagination<Note> getAllNotes(Page page, String projectId) {
        var exists = projectPersistence.existsById(projectId);
        if (!exists) {
            throw new NotFoundException("Project not found", projectId);
        }

        return notePersistence.findAllByProjectId(page, projectId);
    }

}
