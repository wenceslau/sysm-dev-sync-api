package com.sysm.devsync.infrastructure.repositories.tag;


import com.sysm.devsync.domain.models.Tag;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "Tag")
@Table(name = "tags")
public class TagJpaEntity {

    @Id
    private String id;
    private String name;
    private String color;
    private String description;
    private String category;
    private int countUsage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCountUsage() {
        return countUsage;
    }

    public void setCountUsage(int countUsage) {
        this.countUsage = countUsage;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof TagJpaEntity tagJpaEntity)) return false;

        return Objects.equals(id, tagJpaEntity.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public static TagJpaEntity fromModel(Tag tag) {
        TagJpaEntity tagJpaEntity = new TagJpaEntity();
        tagJpaEntity.setId(tag.getId());
        tagJpaEntity.setName(tag.getName());
        tagJpaEntity.setColor(tag.getColor());
        tagJpaEntity.setDescription(tag.getDescription());
        tagJpaEntity.setCategory(tag.getCategory());
        tagJpaEntity.setCountUsage(tag.getCountUsage());
        return tagJpaEntity;
    }

    public static Tag toModel(TagJpaEntity tagJpaEntity) {
        return Tag.build(
                tagJpaEntity.getId(),
                tagJpaEntity.getName(),
                tagJpaEntity.getColor(),
                tagJpaEntity.getDescription(),
                tagJpaEntity.getCategory(),
                tagJpaEntity.getCountUsage()
        );
    }
}
