package com.sysm.devsync.domain;

import com.sysm.devsync.domain.enums.RoleUser;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private String id;
    private Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String email;
    private String passwordHash;
    private String profilePictureUrl;
    private RoleUser roleUser; // ADMIN, MEMBER

    private User(String id, Instant createdAt, Instant updatedAt,
                 String name, String email, String passwordHash,
                 String profilePictureUrl, RoleUser roleUser) {

        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.profilePictureUrl = profilePictureUrl;
        this.roleUser = roleUser;

        validate(name, email, roleUser);
    }

    public User update(String username, String email, RoleUser roleUser) {
        validate(username, email, roleUser);

        this.updatedAt = Instant.now();
        this.name = username;
        this.email = email;
        this.roleUser = roleUser;
        return this;
    }

    public User updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
        return this;
    }

    public User updateProfilePicture(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
        this.updatedAt = Instant.now();
        return this;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public RoleUser getRole() {
        return roleUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void validate(String username, String email, RoleUser roleUser) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (roleUser == null) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
    }

    public final boolean equals(Object o) {
        if (!(o instanceof User user)) return false;

        return Objects.equals(id, user.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public static User create(String username, String email,
                              String profilePictureUrl, RoleUser roleUser) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        return new User(id, now, now, username, email, null, profilePictureUrl, roleUser);
    }

    public static User build(String id, Instant createdAt, Instant updatedAt,
                              String username, String email, String passwordHash,
                              String profilePictureUrl, RoleUser roleUser) {

        return new User(id, createdAt, updatedAt, username, email, passwordHash, profilePictureUrl, roleUser);
    }

}
