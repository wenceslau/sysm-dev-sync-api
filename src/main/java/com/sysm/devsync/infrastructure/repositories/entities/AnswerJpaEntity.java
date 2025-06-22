package com.sysm.devsync.infrastructure.repositories.entities;

import com.sysm.devsync.domain.models.Answer;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity(name = "Answer")
@Table(name = "answers")
public class AnswerJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "is_accepted", nullable = false)
    private boolean isAccepted;

    @ManyToOne(fetch = FetchType.LAZY) // A question can have many answers
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionJpaEntity question;

    @ManyToOne(fetch = FetchType.LAZY) // An answer has one author
    @JoinColumn(name = "author_id", nullable = false)
    private UserJpaEntity author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AnswerJpaEntity() {
    }

    public AnswerJpaEntity(String id) {
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

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean accepted) {
        isAccepted = accepted;
    }

    public QuestionJpaEntity getQuestion() {
        return question;
    }

    public void setQuestion(QuestionJpaEntity question) {
        this.question = question;
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
        if (!(o instanceof AnswerJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "AnswerJpaEntity{" +
               "id='" + id + '\'' +
               ", content='" + content + '\'' +
               ", isAccepted=" + isAccepted +
               ", questionId=" + (question != null ? question.getId() : "null") +
               ", authorId=" + (author != null ? author.getId() : "null") +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    @PrePersist
    protected void onCreate() {
        // Intentionally left blank. Timestamps are managed by the domain layer.
    }

    @PreUpdate
    protected void onUpdate() {
        // Intentionally left blank. Timestamps are managed by the domain layer.
    }

    public static AnswerJpaEntity fromModel(Answer model) {
        if (model == null) {
            return null;
        }
        AnswerJpaEntity entity = new AnswerJpaEntity();
        entity.setId(model.getId());
        entity.setContent(model.getContent());
        entity.setQuestion(new QuestionJpaEntity(model.getQuestionId()));
        entity.setAuthor(new UserJpaEntity(model.getAuthorId()));
        entity.setAccepted(model.isAccepted());
        entity.setCreatedAt(model.getCreatedAt());
        entity.setUpdatedAt(model.getUpdatedAt());
        return entity;
    }

    public static Answer toModel(AnswerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Answer.build(
                entity.getId(),
                entity.getQuestion() != null ? entity.getQuestion().getId() : null,
                entity.getAuthor() != null ? entity.getAuthor().getId() : null,
                entity.getCreatedAt(),
                entity.getContent(),
                entity.isAccepted(),
                entity.getUpdatedAt()
        );
    }
}
