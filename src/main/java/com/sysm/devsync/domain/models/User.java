package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.enums.UserRole;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User extends AbstractModel {

    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    private String name;
    private String email;
    private String passwordHash;
    private String profilePictureUrl;
    private UserRole userRole; // ADMIN, MEMBER

    private User(String id, Instant createdAt, Instant updatedAt,
                 String name, String email, String passwordHash,
                 String profilePictureUrl, UserRole userRole) {

        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.profilePictureUrl = profilePictureUrl;
        this.userRole = userRole;

        validate(name, email, userRole);
    }

    public User update(String username, String email, UserRole userRole) {
        validate(username, email, userRole);

        this.updatedAt = Instant.now();
        this.name = username;
        this.email = email;
        this.userRole = userRole;
        return this;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void updateEmail(String email) {
        if (email == null || email.isBlank() || !email.matches("^[\\w-.]+@[\\w-]+\\.[a-z]{2,}$")) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void updateUserRole(UserRole userRole) {
        if (userRole == null) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        this.userRole = userRole;
        this.updatedAt = Instant.now();
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }

    public void updateProfilePicture(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
        this.updatedAt = Instant.now();
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

    public UserRole getRole() {
        return userRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void validate(String username, String email, UserRole userRole) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (email == null || email.isBlank() || !email.matches("^[\\w-.]+@[\\w-]+\\.[a-z]{2,}$")) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (userRole == null) {
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

    public static User create(String username, String email, UserRole userRole) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();

        return new User(id, now, now, username, email, null, null, userRole);
    }

    public static User build(String id, Instant createdAt, Instant updatedAt,
                             String username, String email, String passwordHash,
                             String profilePictureUrl, UserRole userRole) {

        return new User(id, createdAt, updatedAt, username, email, passwordHash, profilePictureUrl, userRole);
    }

}
