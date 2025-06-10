package com.sysm.devsync.domain;

import java.time.Instant;
import java.util.UUID;

public class User {

    private String id;
    private Instant createdAt;
    private Instant updatedAt;

    private String username;
    private String email;
    private String passwordHash;
    private String profilePictureUrl;
    private ROLE role; // ADMIN, MEMBER

    public User(String username, String email, String profilePictureUrl, ROLE role){
        validate(username, email, role);

        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.username = username;
        this.email = email;
        this.profilePictureUrl = profilePictureUrl;
        this.role = role;

    }

    public User update(String username, String email, ROLE role) {
        validate(username, email, role);

        this.updatedAt = Instant.now();
        this.username = username;
        this.email = email;
        this.role = role;
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

    public String getUsername() {
        return username;
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

    public ROLE getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void validate(String username, String email, ROLE role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
    }

    public static User build(String id, Instant createdAt, Instant updatedAt,
                              String username, String email, String passwordHash,
                              String profilePictureUrl, ROLE role) {

        User user = new User(username, email, profilePictureUrl, role);

        user.id = id;
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;

        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.profilePictureUrl = profilePictureUrl;
        user.role = role;

        return user;
    }

}
