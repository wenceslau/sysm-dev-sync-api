package com.sysm.devsync.infrastructure.controllers.dto.request;

import com.sysm.devsync.domain.enums.QuestionStatus;

public record QuestionCreateUpdate(
        String title,
        String description,
        String projectId,
        QuestionStatus status
) {
}
