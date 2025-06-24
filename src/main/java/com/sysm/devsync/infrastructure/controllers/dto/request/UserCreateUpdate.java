package com.sysm.devsync.infrastructure.controllers.dto.request;

import com.sysm.devsync.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateUpdate(
        @NotBlank(message = "User name must not be blank")
        String name,

        @NotBlank(message = "User email must not be blank")
        @Email(message = "Email should be in a valid format")
        String email,

        String profilePictureUrl,

        @NotNull(message = "User role must not be null")
        UserRole userRole
) {
}
