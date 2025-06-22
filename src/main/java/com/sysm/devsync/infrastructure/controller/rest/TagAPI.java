package com.sysm.devsync.infrastructure.controller.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controller.dto.request.TagCreateUpdate;
import com.sysm.devsync.infrastructure.controller.dto.response.TagResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(value = "tags")
public interface TagAPI {

    // Define methods for tag management here, e.g.:
    // - Create a new tag
    // - Update an existing tag
    // - Delete a tag
    // - List tags with pagination and filtering
    // - Get details of a specific tag by ID

    //     Example method signatures:
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<?> createTag(
            @RequestBody TagCreateUpdate request
    );

    @PutMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<?> updateTag(
            @PathVariable("id") String id,
            @RequestBody TagCreateUpdate request
    );

    @DeleteMapping(value = "/{id}")
    ResponseEntity<?> deleteTag(
            @PathVariable("id") String id
    );

    @GetMapping(value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<TagResponse> getTagById(
            @PathVariable("id") String id
    );

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    Pagination<TagResponse> searchTags(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "terms", required = false, defaultValue = "") String terms
    );

}
