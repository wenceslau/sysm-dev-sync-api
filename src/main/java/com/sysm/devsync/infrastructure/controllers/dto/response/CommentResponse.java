package com.sysm.devsync.infrastructure.controllers.dto.response;

import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;

import java.time.Instant;

public record CommentResponse(
        String id,
        String content,
        TargetType targetType,
        String targetId,
        String authorId,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getAuthorId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
