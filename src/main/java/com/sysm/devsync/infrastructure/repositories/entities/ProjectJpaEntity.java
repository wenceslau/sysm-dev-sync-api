package com.sysm.devsync.infrastructure.repositories.entities;


import com.sysm.devsync.domain.models.Project;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity(name = "Project")
@Table(name = "projecs")
public class ProjectJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true) // Ensure workspace names are unique
    private String name;

    @Column(length = 500) // Optional: Limit the length of the description
    private String description;

    @ManyToOne(fetch = FetchType.LAZY) // A workspace has one owner
    @JoinColumn(name = "workspace_id", nullable = false) // Foreign key column in the 'workspaces' table
    private WorkspaceJpaEntity workspace;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant updatedAt;

    public ProjectJpaEntity(String id) {
        this.id = id;
    }

    public ProjectJpaEntity() {
        // Default constructor for JPA
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkspaceJpaEntity getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceJpaEntity workspace) {
        this.workspace = workspace;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof ProjectJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "WorkspaceJpaEntity{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", workspaceId=" + (workspace != null ? workspace.getId() : "null") + // Avoid NPE and print owner ID
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    @PrePersist
    protected void onCreate() {
        System.out.println("Creating ProejctJpaEntity: " + this);
    }

    @PreUpdate
    protected void onUpdate() {
        System.out.println("Updating ProejctJpaEntity: " + this);
    }

    public static ProjectJpaEntity fromModel(final Project project) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(project.getId());
        entity.setName(project.getName());
        entity.setDescription(project.getDescription());
        entity.setCreatedAt(project.getCreatedAt());
        entity.setUpdatedAt(project.getUpdatedAt());

        if (project.getWorkspaceId() != null) {
            entity.setWorkspace(new WorkspaceJpaEntity(project.getWorkspaceId()));
        }
        return entity;
    }

    public static Project toModel(final ProjectJpaEntity entity) {
        if (entity == null) {
            return null; // Handle a null case gracefully
        }
        return Project.build(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getWorkspace() != null ? entity.getWorkspace().getId() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

