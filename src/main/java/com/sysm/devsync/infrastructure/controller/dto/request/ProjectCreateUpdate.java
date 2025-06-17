package com.sysm.devsync.infrastructure.controller.dto.request;

public record ProjectCreateUpdate(
        String name,
        String description,
        String workspaceId
) {
}
