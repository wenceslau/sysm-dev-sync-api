package com.sysm.devsync.controller.dto.request;

public record NoteCreateUpdate(
        String title,
        String content,
        String projectId
) {
}
