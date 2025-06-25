package com.sysm.devsync.infrastructure.controllers.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnswerCreateUpdate(
        @NotBlank(message = "Answer content must not be blank")
        String content
) {
}

