package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.infrastructure.config.security.IsCommentOwnerOrAdmin;
import com.sysm.devsync.infrastructure.config.security.IsMemberOrAdmin;
import com.sysm.devsync.infrastructure.controllers.dto.request.CommentCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.CommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("comments")
@Tag(name = "Comments")
public interface CommentAPI {

    @IsMemberOrAdmin
    @PostMapping
    @Operation(summary = "Create a new comment on a target (Note, Question, or Answer)")
    @ApiResponse(responseCode = "201", description = "Comment created successfully")
    ResponseEntity<?> createComment(@RequestBody CommentCreateUpdate request);

    @IsMemberOrAdmin
    @GetMapping("/{id}")
    @Operation(summary = "Get a comment by its ID")
    ResponseEntity<CommentResponse> getCommentById(@PathVariable("id") String id);

    @IsMemberOrAdmin
    @GetMapping
    @Operation(summary = "Search for comments with various filters")
    Pagination<CommentResponse> searchComments(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam Map<String, String> filters
    );

    @IsMemberOrAdmin
    @GetMapping("/target/{targetType}/{targetId}")
    @Operation(summary = "Get all comments for a specific target")
    Pagination<CommentResponse> getCommentsByTarget(
            @PathVariable("targetType") TargetType targetType,
            @PathVariable("targetId") String targetId,
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "asc") String direction
    );

    @IsCommentOwnerOrAdmin
    @PutMapping("/{commentId}")
    @Operation(summary = "Update a comment's content")
    ResponseEntity<?> updateComment(
            @PathVariable("commentId") String commentId,
            @RequestBody CommentCreateUpdate request
    );

    @IsCommentOwnerOrAdmin
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment")
    @ApiResponse(responseCode = "204", description = "Comment deleted successfully")
    ResponseEntity<?> deleteComment(@PathVariable("commentId") String commentId);
}
