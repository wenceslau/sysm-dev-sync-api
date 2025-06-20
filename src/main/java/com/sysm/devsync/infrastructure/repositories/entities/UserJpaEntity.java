package com.sysm.devsync.infrastructure.repositories.entities;


import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity(name = "User")
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private String id;
    private String name;
    private String email;
    private String passwordHash;
    private String profilePictureUrl;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    private Instant createdAt;
    private Instant updatedAt;

    public UserJpaEntity() {
    }

    public UserJpaEntity(String id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole userRole) {
        this.role = userRole;
    }

    public Instant getCreatedAt() {
        if (createdAt != null) {
            return createdAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        if (updatedAt != null) {
            return updatedAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof UserJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public static UserJpaEntity fromModel(User user) {
        UserJpaEntity userJpaEntity = new UserJpaEntity();
        userJpaEntity.setId(user.getId());
        userJpaEntity.setName(user.getName());
        userJpaEntity.setEmail(user.getEmail());
        userJpaEntity.setPasswordHash(user.getPasswordHash());
        userJpaEntity.setProfilePictureUrl(user.getProfilePictureUrl());
        userJpaEntity.setRole(user.getRole());
        userJpaEntity.setCreatedAt(user.getCreatedAt());
        userJpaEntity.setUpdatedAt(user.getUpdatedAt());
        return userJpaEntity;
    }

    public static User toModel(UserJpaEntity userJpaEntity) {
        return User.build(
                userJpaEntity.getId(),
                userJpaEntity.getCreatedAt(),
                userJpaEntity.getUpdatedAt(),
                userJpaEntity.getName(),
                userJpaEntity.getEmail(),
                userJpaEntity.getPasswordHash(),
                userJpaEntity.getProfilePictureUrl(),
                userJpaEntity.getRole()
        );
    }

}

