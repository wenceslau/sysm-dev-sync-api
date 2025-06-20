package com.sysm.devsync.infrastructure.repositories.entities;

import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.models.Tag;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sysm.devsync.infrastructure.Utils.iNow;

@Entity(name = "Question")
@Table(name = "questions")
public class QuestionJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserJpaEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false) // Foreign key to ProjectJpaEntity
    private ProjectJpaEntity project;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "question_tags", // Name of the join table
            joinColumns = @JoinColumn(name = "question_id"), // FK for Question in join table
            inverseJoinColumns = @JoinColumn(name = "tag_id") // FK for Tag in join table
    )
    private Set<TagJpaEntity> tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    // Default constructor for JPA
    public QuestionJpaEntity() {
    }

    // Constructor for creating with an ID (e.g., when mapping from domain model)
    public QuestionJpaEntity(String id) {
        this.id = id;
    }


    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public UserJpaEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserJpaEntity author) {
        this.author = author;
    }

    public ProjectJpaEntity getProject() {
        return project;
    }

    public void setProject(ProjectJpaEntity project) {
        this.project = project;
    }

    public Set<TagJpaEntity> getTags() {
        return tags;
    }

    public void setTags(Set<TagJpaEntity> tags) {
        this.tags = tags;
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

    @PrePersist
    protected void onCreate() {
    }

    @PreUpdate
    protected void onUpdate() {
    }

    public final boolean equals(Object o) {
        if (!(o instanceof QuestionJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "QuestionJpaEntity{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", status=" + status +
               ", authorId=" + (author != null ? author.getId() : "null") +
               ", projectId=" + (project != null ? project.getId() : "null") +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }

    public static QuestionJpaEntity fromModel(Question question) {
        QuestionJpaEntity entity = new QuestionJpaEntity(question.getId());
        entity.setTitle(question.getTitle());
        entity.setDescription(question.getDescription());
        entity.setStatus(question.getStatus());
        entity.setAuthor(new UserJpaEntity(question.getAuthorId()));
        entity.setProject(new ProjectJpaEntity(question.getProjectId()));

        if (question.getTagsId() != null) {
            Set<TagJpaEntity> tagEntities = question.getTagsId().stream()
                    .map(TagJpaEntity::new)
                    .collect(Collectors.toSet());
            entity.setTags(tagEntities);
        }

        entity.setCreatedAt(question.getCreatedAt());
        entity.setUpdatedAt(question.getUpdatedAt());

        return entity;
    }

    public static Question toModel(QuestionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<String> tagIds = entity.getTags() != null ?
            entity.getTags().stream()
                    .map(TagJpaEntity::getId)
                    .collect(Collectors.toSet()) :
            Set.of();

        return Question.build(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getTitle(),
                entity.getDescription(),
                tagIds,
                entity.getProject() != null ? entity.getProject().getId() : null,
                entity.getAuthor() != null ? entity.getAuthor().getId() : null,
                entity.getStatus()
        );
    }
}

