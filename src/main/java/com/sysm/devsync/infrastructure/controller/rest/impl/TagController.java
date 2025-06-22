package com.sysm.devsync.infrastructure.controller.rest.impl;

import com.sysm.devsync.application.TagService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.infrastructure.controller.dto.request.TagCreateUpdate;
import com.sysm.devsync.infrastructure.controller.dto.response.TagResponse;
import com.sysm.devsync.infrastructure.controller.rest.TagAPI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class TagController implements TagAPI {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public ResponseEntity<?> createTag(@Valid TagCreateUpdate request) {

        var response = tagService.createTag(request);

        return ResponseEntity
                .created(URI.create(String.format("/tags/%s", response.id())))
                .body(response);
    }

    @Override
    public ResponseEntity<?> updateTag(String id, @Valid TagCreateUpdate request) {
        tagService.updateTag(id, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public ResponseEntity<?> deleteTag(String id) {
        tagService.deleteTag(id);
        return ResponseEntity
                .noContent()
                .build();
    }

    @Override
    public ResponseEntity<TagResponse> getTagById(String id) {
        var tag = tagService.getTagById(id);
        return ResponseEntity
                .ok(TagResponse.from(tag));
    }

    @Override
    public Pagination<TagResponse> searchTags(int pageNumber, int pageSize, String sort, String direction, String terms) {
        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = new SearchQuery(page, terms);

        var pagination = tagService.searchTags(searchQuery);

        return pagination.map(TagResponse::from);
    }
}
