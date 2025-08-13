package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.models.to.WorkspaceTO;

import java.time.Instant;

public record ProjectResponse(
        String id,
        String name,
        String description,
        WorkspaceTO workspace,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getWorkspace(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
