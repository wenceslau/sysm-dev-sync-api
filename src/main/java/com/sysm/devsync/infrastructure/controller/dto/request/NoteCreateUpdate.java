package com.sysm.devsync.infrastructure.controller.dto.request;

public record NoteCreateUpdate(
        String title,
        String content,
        String projectId
) {
}
