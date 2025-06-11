package com.sysm.devsync.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Workspace {

    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String description;
    private boolean isPrivate;
    private User ownerId;
    private Set<User> members;

    private Workspace(String id, Instant createdAt, Instant updatedAt,
                      String name, String description, boolean isPrivate,
                      User ownerId, Set<User> members) {

        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
        this.ownerId = ownerId;
        this.members = members;

        validate(name, description);

        if (ownerId == null) {
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

    public Workspace changeOwner(User newOwner) {
        if (newOwner == null) {
            throw new IllegalArgumentException("New owner cannot be null");
        }

        this.updatedAt = Instant.now();
        this.ownerId = newOwner;

        return this;
    }

    public Workspace setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
        this.updatedAt = Instant.now();
        return this;
    }

    public void addMember(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (members == null) {
            members = new HashSet<>();
        }

        this.members.add(user);
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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

    public User getOwnerId() {
        return ownerId;
    }

    public Set<User> getMembers() {
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
                                   User ownerId, Set<User> members) {

        String id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new Workspace(
                id,
                now,
                now,
                name,
                description,
                isPrivate,
                ownerId,
                members
        );
    }

    public static Workspace build(String id, Instant createdAt, Instant updatedAt,
                                  String name, String description, boolean isPrivate,
                                  User ownerId, Set<User> members) {

        return new Workspace(
                id,
                createdAt,
                updatedAt,
                name,
                description,
                isPrivate,
                ownerId,
                members
        );
    }

}
