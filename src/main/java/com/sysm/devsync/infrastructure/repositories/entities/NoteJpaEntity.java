package com.sysm.devsync.infrastructure.repositories.entities;

import com.sysm.devsync.domain.models.Note;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity(name = "Note")
@Table(name = "notes")
public class NoteJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "version", nullable = false)
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectJpaEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserJpaEntity author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "note_tags", // Name of the join table
        joinColumns = @JoinColumn(name = "note_id"), // FK for Note in join table
        inverseJoinColumns = @JoinColumn(name = "tag_id") // FK for Tag in join table
    )
    private Set<TagJpaEntity> tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NoteJpaEntity() {
    }

    public NoteJpaEntity(String id) {
        this.id = id;
    }

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public ProjectJpaEntity getProject() {
        return project;
    }

    public void setProject(ProjectJpaEntity project) {
        this.project = project;
    }

    public UserJpaEntity getAuthor() {
        return author;
    }

    public void setAuthor(UserJpaEntity author) {
        this.author = author;
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
        // Intentionally left blank. Timestamps are managed by the domain layer.
    }

    @PreUpdate
    protected void onUpdate() {
        // Intentionally left blank. Timestamps are managed by the domain layer.
    }

    public final boolean equals(Object o) {
        if (!(o instanceof NoteJpaEntity that)) return false;

        return Objects.equals(id, that.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "NoteJpaEntity{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", version='" + version + '\'' +
                ", projectId=" + (project != null ? project.getId() : "null") +
                ", authorId=" + (author != null ? author.getId() : "null") +
                ", tagsSize=" + (tags != null ? tags.size() : "0") +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static NoteJpaEntity fromModel(final Note note) {
        NoteJpaEntity entity = new NoteJpaEntity(note.getId());
        entity.setTitle(note.getTitle());
        entity.setContent(note.getContent());
        entity.setVersion(note.getVersion());
        entity.setProject(new ProjectJpaEntity(note.getProjectId()));
        entity.setAuthor(new UserJpaEntity(note.getAuthorId()));
        entity.setCreatedAt(note.getCreatedAt());
        entity.setUpdatedAt(note.getUpdatedAt());

        // Convert tags to TagJpaEntity
        if (note.getTagsId() != null) {
            Set<TagJpaEntity> tagEntities = note.getTagsId().stream()
                .map(TagJpaEntity::new)
                .collect(Collectors.toSet());
            entity.setTags(tagEntities);
        }

        return entity;

    }

    public static Note toModel(NoteJpaEntity entity) {

        return Note.build(
            entity.getId(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getTitle(),
            entity.getContent(),
            entity.getTags().stream().map(TagJpaEntity::getId).collect(Collectors.toSet()),
            entity.getProject() != null ? entity.getProject().getId() : null,
            entity.getAuthor() != null ? entity.getAuthor().getId() : null,
            entity.getVersion()
        );
    }
}
