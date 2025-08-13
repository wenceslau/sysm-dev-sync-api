package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.models.to.WorkspaceTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Project extends AbstractModel {

    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;
    private String name;
    private String description;
    private WorkspaceTO workspace;

    private Project(String id, Instant createdAt, Instant updatedAt, String name, String description, WorkspaceTO workspace) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.workspace = workspace;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validate(name, description);
    }

    private void validate(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Project description cannot be null or blank");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WorkspaceTO getWorkspace() {
        return workspace;
    }

    public Instant getCreatedAt() {
        if (createdAt != null) {
            return createdAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }

    public Instant getUpdatedAt() {
        if (updatedAt != null) {
            return updatedAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }
    public Project update(String name, String description) {
        validate(name, description);
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
        return this;
    }

    public void changeWorkspace(String newWorkspaceId) {
        if (newWorkspaceId == null || newWorkspaceId.isBlank()) {
            throw new IllegalArgumentException("New workspace ID cannot be null or blank");
        }
        this.workspace = WorkspaceTO.of(newWorkspaceId);
        this.updatedAt = Instant.now();
    }

    public static Project create(String name, String description, WorkspaceTO workspace) {
        String id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new Project(id, now, now, name, description, workspace);
    }

    public static Project create(String name, String description, String workspaceId) {
        return create(name, description, WorkspaceTO.of(workspaceId));
    }

    public static Project build(String id, String name, String description, WorkspaceTO workspace, Instant createdAt, Instant updatedAt) {
        return new Project(id, createdAt, updatedAt, name, description, workspace);
    }

    public static Project build(String id, String name, String description, String workspaceId, Instant createdAt, Instant updatedAt) {
        return build(id, name, description, WorkspaceTO.of(workspaceId), createdAt, updatedAt);
    }

}
