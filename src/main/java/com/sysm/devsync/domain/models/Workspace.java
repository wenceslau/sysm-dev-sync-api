package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.models.to.UserTO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Workspace extends AbstractModel {

    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String description;
    private boolean isPrivate;
    private UserTO owner;
    private Set<UserTO> members;

    private Workspace(String id, Instant createdAt, Instant updatedAt,
                      String name, String description, boolean isPrivate,
                      UserTO owner, Set<UserTO> members) {

        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.owner = owner;
        this.members = members;

        validate(name, description);

        if (owner == null) {
            throw new IllegalArgumentException("Owner cannot be null");
        }

    }

    public void validate(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Workspace name cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Workspace description cannot be null or blank");
        }
    }

    public Workspace update(String name, String description) {

        validate(name, description);

        this.updatedAt = Instant.now();
        this.name = name;
        this.description = description;

        return this;
    }

    public void changeOwner(String newOwnerId) {
        if (newOwnerId == null) {
            throw new IllegalArgumentException("New owner cannot be null");
        }

        this.updatedAt = Instant.now();
        this.owner = UserTO.of(newOwnerId);
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        this.updatedAt = Instant.now();
    }

    public void addMember(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (members == null) {
            members = new HashSet<>();
        }

        this.members.add(UserTO.of(userId));
    }

    public void addMember(String userId, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (members == null) {
            members = new HashSet<>();
        }

        this.members.add(UserTO.of(userId, name));
    }

    public void removeMember(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (members == null) {
            throw new IllegalArgumentException("Members cannot be null");
        }
        this.members.removeIf(u-> u.id().equals(userId));
    }

    public String getId() {
        return id;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public UserTO getOwner() {
        return owner;
    }

    public Set<String> getMembersId() {
        return members.stream()
                .map(UserTO::id)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<UserTO> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public final boolean equals(Object o) {
        if (!(o instanceof Workspace that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public static Workspace create(String name, String description, boolean isPrivate,
                                   UserTO owner) {

        String id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new Workspace(
                id,
                now,
                now,
                name,
                description,
                isPrivate,
                owner,
                new HashSet<>()
        );
    }

    public static Workspace create(String name, String description, boolean isPrivate,
                                   String ownerId) {

        return create(name, description, isPrivate, UserTO.of(ownerId));
    }

    public static Workspace build(String id, Instant createdAt, Instant updatedAt,
                                  String name, String description, boolean isPrivate,
                                  UserTO ownerId, Set<UserTO> members) {
        return new Workspace(
                id,
                createdAt,
                updatedAt,
                name,
                description,
                isPrivate,
                ownerId,
                members != null ? new HashSet<>(members) : new HashSet<>()
        );
    }

}
