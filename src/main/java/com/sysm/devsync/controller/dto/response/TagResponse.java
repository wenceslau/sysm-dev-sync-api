package com.sysm.devsync.controller.dto.response;

public record TagResponse(
        String id,
        String name,
        String color,
        String description
) {
}
