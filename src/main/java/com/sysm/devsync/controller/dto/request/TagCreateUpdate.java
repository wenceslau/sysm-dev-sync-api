package com.sysm.devsync.controller.dto.request;

public record TagCreateUpdate(
        String name,
        String color,
        String description,
        String category
) {
}
