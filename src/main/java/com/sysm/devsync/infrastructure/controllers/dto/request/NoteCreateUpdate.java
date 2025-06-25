package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NoteCreateUpdate(
        @NotBlank(message = "Note title must not be blank")
        String title,

        @NotBlank(message = "Note content must not be blank")
        String content,

        @NotBlank(message = "Project ID must be provided")
        String projectId
) {}
