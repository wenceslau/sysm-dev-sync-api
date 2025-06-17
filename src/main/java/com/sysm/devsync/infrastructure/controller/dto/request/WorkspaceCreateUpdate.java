package com.sysm.devsync.infrastructure.controller.dto.request;

public record WorkspaceCreateUpdate(
        String name,
        String description,
        boolean isPrivate
) {
}
