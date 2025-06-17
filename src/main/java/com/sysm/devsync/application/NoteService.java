package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.NoteCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.Pageable;
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
            throw new IllegalArgumentException("Project not found");
        }

        var userExists = userPersistence.existsById(authorId);
        if (!userExists) {
            throw new IllegalArgumentException("User not found");
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
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        note.update(
                noteUpdate.title(),
                noteUpdate.content()
        );

        notePersistence.update(note);
    }

    public void updateNoteContent(String noteId, NoteCreateUpdate noteUpdate) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        note.updateContent(noteUpdate.content());

        notePersistence.update(note);
    }

    public void addTagToNote(String noteId, String tagId) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        var exists = tagPersistence.existsById(tagId);
        if (!exists) {
            throw new IllegalArgumentException("Tag not found");
        }

        note.addTag(tagId);
        notePersistence.update(note);
    }

    public void removeTagFromNote(String noteId, String tagId) {
        var note = notePersistence.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        var exists = tagPersistence.existsById(tagId);
        if (!exists) {
            throw new IllegalArgumentException("Tag not found");
        }

        note.removeTag(tagId);
        notePersistence.update(note);
    }

    public void deleteNote(String noteId) {
        var exists = notePersistence.existsById(noteId);

        if (!exists) {
            throw new IllegalArgumentException("Note not found");
        }

        notePersistence.deleteById(noteId);
    }

    public Note getNoteById(String noteId) {
        return notePersistence.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));
    }

    public Pagination<Note> getAllNotes(SearchQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("Invalid query parameters");
        }
        return notePersistence.findAll(query);
    }

    public Pagination<Note> getAllNotes(Pageable pageable, String projectId) {
        var exists = projectPersistence.existsById(projectId);
        if (!exists) {
            throw new IllegalArgumentException("Project not found");
        }

        return notePersistence.findAllByProjectId(pageable, projectId);
    }

}
