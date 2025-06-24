package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QuestionCreateUpdate(
        @NotBlank(message = "Question title must not be blank")
        String title,

        @NotBlank(message = "Question description must not be blank")
        String description,

        @NotBlank(message = "Project ID must be provided")
        String projectId
) {}
