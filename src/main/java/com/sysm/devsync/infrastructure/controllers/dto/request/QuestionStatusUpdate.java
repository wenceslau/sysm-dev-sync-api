package com.sysm.devsync.infrastructure.controllers.dto.request;

import com.sysm.devsync.domain.enums.QuestionStatus;
import jakarta.validation.constraints.NotNull;

public record QuestionStatusUpdate(
        @NotNull(message = "Status must not be null")
        QuestionStatus status
) {}
