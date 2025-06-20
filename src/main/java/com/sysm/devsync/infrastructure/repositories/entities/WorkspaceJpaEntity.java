package com.sysm.devsync.infrastructure.repositories.entities;


import com.sysm.devsync.domain.models.Workspace;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "Workspace")
@Table(name = "workspaces")
public class WorkspaceJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true) // Ensure workspace names are unique
    private String name;

    @Column(length = 500) // Optional: Limit the length of the description
    private String description;

    @ManyToOne(fetch = FetchType.LAZY) // A workspace has one owner
    @JoinColumn(name = "owner_id", nullable = false) // Foreign key column in the 'workspaces' table
    private UserJpaEntity owner;

    @ManyToMany(fetch = FetchType.LAZY) // A workspace can have many members, and a user can be in many workspaces
    @JoinTable(
            name = "workspace_members", // Name of the intermediary join table
            joinColumns = @JoinColumn(name = "workspace_id"), // Foreign key for Workspace in the join table
            inverseJoinColumns = @JoinColumn(name = "user_id") // Foreign key for User in the join table
    )
    private Set<UserJpaEntity> members = new HashSet<>(); // Initialize to avoid NullPointerExceptions

    @Column(name = "is_private") // Good practice to explicitly name boolean columns
    private boolean isPrivate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WorkspaceJpaEntity(String id) {
        this.id = id;
    }

    public WorkspaceJpaEntity() {
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

    public UserJpaEntity getOwner() {
        return owner;
    }

    public void setOwner(UserJpaEntity owner) {
        this.owner = owner;
    }

    public Set<UserJpaEntity> getMembers() {
        return members;
    }

    public void setMembers(Set<UserJpaEntity> members) {
        this.members = members;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof WorkspaceJpaEntity that)) return false;

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
               ", ownerId=" + (owner != null ? owner.getId() : "null") + // Avoid NPE and print owner ID
               ", memberCount=" + (members != null ? members.size() : 0) + // Print member count
               ", isPrivate=" + isPrivate +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    // Lifecycle Callbacks for createdAt and updatedAt (Optional but common)
    @PrePersist
    protected void onCreate() {
        System.out.println("Creating WorkspaceJpaEntity: " + this);
        createdAt = updatedAt = LocalDateTime.now(ZoneOffset.UTC);

    }

    @PreUpdate
    protected void onUpdate() {
        System.out.println("Updating WorkspaceJpaEntity: " + this);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public static WorkspaceJpaEntity fromModel(Workspace workspace) {
        if (workspace == null) {
            return null; // Handle a null case gracefully
        }
        WorkspaceJpaEntity workspaceJpaEntity = new WorkspaceJpaEntity();
        workspaceJpaEntity.setId(workspace.getId());
        workspaceJpaEntity.setName(workspace.getName());
        workspaceJpaEntity.setDescription(workspace.getDescription());
        workspaceJpaEntity.setOwner(new UserJpaEntity(workspace.getOwnerId()));
        workspaceJpaEntity.setMembers(
                workspace.getMembersId().stream()
                        .map(UserJpaEntity::new)
                        .collect(Collectors.toSet())
        );
        workspaceJpaEntity.setPrivate(workspace.isPrivate());
        workspaceJpaEntity.setCreatedAt(LocalDateTime.ofInstant(workspace.getCreatedAt(), ZoneId.systemDefault()));
        workspaceJpaEntity.setUpdatedAt(LocalDateTime.ofInstant(workspace.getUpdatedAt(), ZoneId.systemDefault()));

        return workspaceJpaEntity;
    }

    public static Workspace toModel(WorkspaceJpaEntity workspaceJpaEntity) {
        if (workspaceJpaEntity == null) {
            return null; // Handle a null case gracefully
        }
        return Workspace.build(
                workspaceJpaEntity.getId(),
                workspaceJpaEntity.getCreatedAt().toInstant(ZoneOffset.UTC),
                workspaceJpaEntity.getUpdatedAt().toInstant(ZoneOffset.UTC),
                workspaceJpaEntity.getName(),
                workspaceJpaEntity.getDescription(),
                workspaceJpaEntity.isPrivate(),
                workspaceJpaEntity.getOwner().getId(),
                workspaceJpaEntity.getMembers().stream()
                        .map(UserJpaEntity::getId)
                        .collect(Collectors.toSet())
        );
    }
}

