package com.sysm.devsync.infrastructure.controllers.dto.request;

import com.sysm.devsync.domain.enums.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateUpdate(
        @NotNull(message = "Target type must not be null")
        TargetType targetType,

        @NotBlank(message = "Target ID must not be blank")
        String targetId,

        @NotBlank(message = "Comment content must not be blank")
        String content
) {}
