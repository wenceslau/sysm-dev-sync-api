package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.infrastructure.controllers.dto.request.CommentCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("comments")
@Tag(name = "Comments")
public interface CommentAPI {

    @PostMapping()
    @Operation(summary = "Create a new comment")
    @ApiResponse(responseCode = "201", description = "Comment created successfully")
    ResponseEntity<?> createComment(@RequestBody CommentCreateUpdate request);

    @GetMapping("/{id}")
    @Operation(summary = "Get a comment by its ID")
    ResponseEntity<CommentResponse> getCommentById(@PathVariable("id") String id);

    @GetMapping()
    @Operation(summary = "Search for comments with pagination and filters")
    Pagination<CommentResponse> searchComments(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "terms", required = false) String terms
    );

    // The getCommentsByTarget endpoint is no longer needed and can be removed.
    // Its functionality is now handled by searchComments.

    @PutMapping("/{id}")
    @Operation(summary = "Update a comment's content")
    ResponseEntity<?> updateComment(@PathVariable("id") String id, @RequestBody CommentCreateUpdate request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment")
    @ApiResponse(responseCode = "204", description = "Comment deleted successfully")
    ResponseEntity<?> deleteComment(@PathVariable("id") String id);
}
