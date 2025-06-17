package com.sysm.devsync.infrastructure.controller.dto.response;

import com.sysm.devsync.domain.enums.UserRole;

public record UserResponse(
        String id,
        String username,
        String email,
        String profilePictureUrl,
        UserRole role
) {
}
