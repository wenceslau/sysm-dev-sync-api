package com.sysm.devsync.controller.dto.request;

public record WorkspaceCreateUpdate(
        String name,
        String description,
        boolean isPrivate
) {
}
