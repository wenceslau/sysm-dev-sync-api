package com.sysm.devsync.infrastructure.controller.dto.response;

public record TagResponse(
        String id,
        String name,
        String color,
        String description
) {
}
