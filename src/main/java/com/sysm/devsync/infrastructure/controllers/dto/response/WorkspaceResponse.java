package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Workspace;

import java.time.Instant;
import java.util.Set;

public record WorkspaceResponse(
        String id,
        String name,
        String description,
        boolean isPrivate,
        String ownerId,
        Set<String> membersId,
        long projectCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkspaceResponse from(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.isPrivate(),
                workspace.getOwnerId(),
                workspace.getMembersId(),
                0,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
    public static WorkspaceResponse from(Workspace workspace, long projectCount) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.isPrivate(),
                workspace.getOwnerId(),
                workspace.getMembersId(),
                projectCount,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
