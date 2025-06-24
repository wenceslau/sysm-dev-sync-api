package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Project;

import java.time.Instant;

public record ProjectResponse(
        String id,
        String name,
        String description,
        String workspaceId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getWorkspaceId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
