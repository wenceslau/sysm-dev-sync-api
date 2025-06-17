package com.sysm.devsync.infrastructure.controller.dto.request;

import com.sysm.devsync.domain.enums.UserRole;

public record UserCreateUpdate(
        String name,
        String email,
        String profilePictureUrl,
        UserRole userRole
) {
}
