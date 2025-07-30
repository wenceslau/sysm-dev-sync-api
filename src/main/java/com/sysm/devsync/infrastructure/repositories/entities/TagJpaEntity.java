package com.sysm.devsync.infrastructure.repositories.entities;


import com.sysm.devsync.domain.models.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity(name = "Tag")
@Table(name = "tags")
public class TagJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column()
    private String color;

    @Column(length = 500)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "amount_used", nullable = false)
    private int amountUsed;

    public TagJpaEntity() {
    }

    public TagJpaEntity(String id) {
        this.id = id;
    }

    public TagJpaEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

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

    public int getAmountUsed() {
        return amountUsed;
    }

    public void setAmountUsed(int amountUsed) {
        this.amountUsed = amountUsed;
    }

    public final boolean equals(Object o) {
        if (!(o instanceof TagJpaEntity tagJpaEntity)) return false;

        return Objects.equals(id, tagJpaEntity.id);
    }

    public final int hashCode() {
        return Objects.hashCode(id);
    }

    public final String toString() {
        return "TagJpaEntity{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", color='" + color + '\'' +
               ", description='" + description + '\'' +
               ", category='" + category + '\'' +
               ", countUsage=" + amountUsed +
               '}';
    }

    public static TagJpaEntity fromModel(Tag tag) {
        TagJpaEntity tagJpaEntity = new TagJpaEntity();
        tagJpaEntity.setId(tag.getId());
        tagJpaEntity.setName(tag.getName());
        tagJpaEntity.setColor(tag.getColor());
        tagJpaEntity.setDescription(tag.getDescription());
        tagJpaEntity.setCategory(tag.getCategory());
        tagJpaEntity.setAmountUsed(tag.getAmountUsed());
        return tagJpaEntity;
    }

    public static Tag toModel(TagJpaEntity tagJpaEntity) {
        return Tag.build(
                tagJpaEntity.getId(),
                tagJpaEntity.getName(),
                tagJpaEntity.getColor(),
                tagJpaEntity.getDescription(),
                tagJpaEntity.getCategory(),
                tagJpaEntity.getAmountUsed()
        );
    }
}
