package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateUpdate(
        @NotBlank(message = "Project name must not be blank")
        String name,

        @NotBlank(message = "Project description must not be blank")
        String description,

        @NotBlank(message = "Workspace ID must be provided")
        String workspaceId
) {}
