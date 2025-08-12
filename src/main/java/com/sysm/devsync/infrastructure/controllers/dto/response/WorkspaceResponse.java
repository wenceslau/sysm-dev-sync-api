package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.models.to.UserTO;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public record WorkspaceResponse(
        String id,
        String name,
        String description,
        boolean isPrivate,
        UserTO owner,
        Set<UserTO> members,
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
                workspace.getOwner(),
                workspace.getMembers(),
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
                workspace.getOwner(),
                workspace.getMembers(),
                projectCount,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

}


