package com.sysm.devsync.application;

import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.TagCreateUpdate;
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
                .orElseThrow(() -> new NotFoundException("Tag not found", tagId));

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
        if (!tagPersistence.existsById(tagId)) {
            throw new NotFoundException("Tag not found", tagId);
        }
        tagPersistence.deleteById(tagId);
    }

    public Tag getTagById(String tagId) {
        return tagPersistence.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Tag not found", tagId));
    }

    public Pagination<Tag> searchTags(SearchQuery query) {
        return tagPersistence.findAll(query);
    }

}
