package com.sysm.devsync.controller.dto.request;

public record ProjectCreateUpdate(
        String name,
        String description,
        String workspaceId
) {
}
