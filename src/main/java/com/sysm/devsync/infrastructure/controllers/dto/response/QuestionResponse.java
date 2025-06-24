package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.models.Question;

import java.time.Instant;
import java.util.Set;

public record QuestionResponse(
        String id,
        String title,
        String description,
        QuestionStatus status,
        String authorId,
        String projectId,
        Set<String> tagsId,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getTitle(),
                question.getDescription(),
                question.getStatus(),
                question.getAuthorId(),
                question.getProjectId(),
                question.getTagsId(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
