package com.sysm.devsync.infrastructure.controller.dto.request;

public record TagCreateUpdate(
        String name,
        String color,
        String description,
        String category
) {
}
