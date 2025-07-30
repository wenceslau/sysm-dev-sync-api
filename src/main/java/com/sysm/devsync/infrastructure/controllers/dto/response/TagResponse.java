package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Tag;

public record TagResponse(
        String id,
        String name,
        String color,
        String description,
        String category,
        int amountUsed
) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getDescription(),
                tag.getCategory(),
                tag.getAmountUsed()
        );
    }
}
