package com.sysm.devsync.infrastructure.controllers.dto.request;

public record ProjectCreateUpdate(
        String name,
        String description,
        String workspaceId
) {
}
