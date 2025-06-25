package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.models.Answer;
import java.time.Instant;

public record AnswerResponse(
        String id,
        String content,
        boolean isAccepted,
        String questionId,
        String authorId,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnswerResponse from(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getContent(),
                answer.isAccepted(),
                answer.getQuestionId(),
                answer.getAuthorId(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}
