package com.sysm.devsync.domain.models;

import com.sysm.devsync.domain.enums.TargetType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Comment extends AbstractModel {

    private final String id;
    private final TargetType targetType; // NOTE, QUESTION, ANSWER
    private final String targetId; // Reference to the target (Note, Question, Answer)
    private final String authorId;
    private final Instant createdAt;

    private String content;
    private Instant updatedAt;

    private Comment(String id, TargetType targetType, String targetId,
                    String authorId, Instant createdAt, String content,
                    Instant updatedAt) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.content = content;
        this.updatedAt = updatedAt;
        validate();
    }

    public void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (authorId == null || authorId.isBlank()) {
            throw new IllegalArgumentException("Author ID cannot be null or empty");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("Target ID cannot be null or empty");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
    }

    public Comment update(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        this.content = content;
        this.updatedAt = Instant.now();
        return this;
    }

    public String getId() {
        return id;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        if (createdAt != null) {
            return createdAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }

    public Instant getUpdatedAt() {
        if (updatedAt != null) {
            return updatedAt.truncatedTo(ChronoUnit.MILLIS);
        }
        return null;
    }

    public static Comment create(TargetType targetType, String targetId, String authorId, String content) {
        String id = java.util.UUID.randomUUID().toString();
        Instant now = Instant.now();
        return new Comment(id, targetType, targetId, authorId, now, content, now);
    }

    public static Comment build(String id, TargetType targetType, String targetId,
                                String authorId, Instant createdAt, String content,
                                Instant updatedAt) {
        return new Comment(id, targetType, targetId, authorId, createdAt, content, updatedAt);
    }
}
