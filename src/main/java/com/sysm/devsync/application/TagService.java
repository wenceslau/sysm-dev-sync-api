package com.sysm.devsync.application;

import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.TagCreateUpdate;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import org.springframework.util.StringUtils;

public class TagService {

    private final TagPersistencePort tagPersistence;

    public TagService(TagPersistencePort tagPersistence) {
        this.tagPersistence = tagPersistence;
    }

    public CreateResponse createTag(TagCreateUpdate tagCreateUpdate) {
        Tag tag = Tag.create(tagCreateUpdate.name(), tagCreateUpdate.color());

        if (StringUtils.hasText(tagCreateUpdate.description())) {
            tag.updateDescription(tagCreateUpdate.description());
        }

        if (StringUtils.hasText(tagCreateUpdate.category())) {
            tag.updateCategory(tagCreateUpdate.category());
        }

        tagPersistence.create(tag);

        return new CreateResponse(tag.getId());
    }

    public void updateTag(String tagId, TagCreateUpdate tagCreateUpdate) {
        Tag tag = tagPersistence.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        tag.update(tagCreateUpdate.name(), tagCreateUpdate.color());

        if (StringUtils.hasText(tagCreateUpdate.description())) {
            tag.updateDescription(tagCreateUpdate.description());
        }

        if (StringUtils.hasText(tagCreateUpdate.category())) {
            tag.updateCategory(tagCreateUpdate.category());
        }

        tagPersistence.update(tag);
    }

    public void deleteTag(String tagId) {
        tagPersistence.deleteById(tagId);
    }

    public Tag getTagById(String tagId) {
        return tagPersistence.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    public Page<Tag> getAllTags(SearchQuery query) {
        return tagPersistence.findAll(query);
    }

}
