package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.enums.UserRole;
import com.sysm.devsync.domain.models.User;

public record UserResponse(
        String id,
        String username,
        String email,
        String profilePictureUrl,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfilePictureUrl(),
                user.getRole()
        );
    }
}
