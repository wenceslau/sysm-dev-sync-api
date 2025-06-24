package com.sysm.devsync.infrastructure.controllers.dto.request;

public record NoteCreateUpdate(
        String title,
        String content,
        String projectId
) {
}
