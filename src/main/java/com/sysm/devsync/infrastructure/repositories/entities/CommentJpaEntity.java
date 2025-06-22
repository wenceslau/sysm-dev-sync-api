package com.sysm.devsync.infrastructure.repositories.entities;

import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity(name = "Comment")
@Table(name = "comment")
public class CommentJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserJpaEntity author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CommentJpaEntity() {
    }

    public CommentJpaEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public UserJpaEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserJpaEntity author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof CommentJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "CommentJpaEntity{" +
               "id='" + id + '\'' +
               ", content='" + content + '\'' +
               ", targetType=" + targetType +
               ", targetId='" + targetId + '\'' +
               ", authorId=" + (author != null ? author.getId() : "null") +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    public static CommentJpaEntity fromModel(final Comment comment) {
        if (comment == null) {
            return null;
        }

        CommentJpaEntity entity = new CommentJpaEntity(comment.getId());
        entity.setContent(comment.getContent());
        entity.setTargetType(comment.getTargetType());
        entity.setTargetId(comment.getTargetId());
        entity.setCreatedAt(comment.getCreatedAt());
        entity.setUpdatedAt(comment.getUpdatedAt());
        UserJpaEntity author = new UserJpaEntity(comment.getAuthorId());
        entity.setAuthor(author);

        return entity;
    }

    public static Comment toModel(CommentJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Comment.build(
                entity.getId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getAuthor() != null ? entity.getAuthor().getId() : null,
                entity.getCreatedAt(),
                entity.getContent(),
                entity.getUpdatedAt()
        );
    }
}
