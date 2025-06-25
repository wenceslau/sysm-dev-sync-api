package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Note;
import java.time.Instant;
import java.util.Set;

public record NoteResponse(
        String id,
        String title,
        String content,
        int version,
        String projectId,
        String authorId,
        Set<String> tagsId,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getVersion(),
                note.getProjectId(),
                note.getAuthorId(),
                note.getTagsId(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
