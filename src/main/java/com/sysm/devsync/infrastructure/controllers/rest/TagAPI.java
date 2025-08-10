package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.config.security.IsMemberOrAdmin;
import com.sysm.devsync.infrastructure.controllers.dto.request.TagCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping(value = "tags")
@Tag(name = "Tags")
public interface TagAPI {

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a new tag")
    @ApiResponse(responseCode = "201", description = "Tag created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    ResponseEntity<?> createTag(
            @RequestBody TagCreateUpdate request
    );

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Update an existing tag")
    @ApiResponse(responseCode = "204", description = "Tag updated successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    ResponseEntity<?> updateTag(
            @PathVariable("id") String id,
            @RequestBody TagCreateUpdate request
    );

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Delete a tag by its ID")
    @ApiResponse(responseCode = "204", description = "Tag deleted successfully")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    ResponseEntity<?> deleteTag(
            @PathVariable("id") String id
    );


    @IsMemberOrAdmin
    @GetMapping(value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get a tag by its ID")
    @ApiResponse(responseCode = "200", description = "Tag found")
    @ApiResponse(responseCode = "404", description = "Tag not found")
    ResponseEntity<TagResponse> getTagById(
            @PathVariable("id") String id
    );

    @IsMemberOrAdmin
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search for tags with pagination")
    Pagination<TagResponse> searchTags(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "direction", required = false, defaultValue = "asc") String direction,
            @RequestParam(name = "queryType", required = false, defaultValue = "or") String queryType,
            @RequestParam Map<String, String> filters
    );

}
