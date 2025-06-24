package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkspaceCreateUpdate(
        @NotBlank(message = "Workspace name must not be blank")
        String name,

        @NotBlank(message = "Workspace description must not be blank")
        String description,

        @NotNull(message = "Privacy setting must be provided")
        Boolean isPrivate
) {
}
