package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TagCreateUpdate(
        @NotBlank(message = "Tag name must not be blank") String name,
        @NotBlank(message = "Tag color must not be blank")String color,
        String description,
        String category
) {
}
