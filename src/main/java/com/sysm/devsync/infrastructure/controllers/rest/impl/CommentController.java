package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.CommentService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.infrastructure.controllers.dto.request.CommentCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.CommentResponse;
import com.sysm.devsync.infrastructure.controllers.rest.CommentAPI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
public class CommentController implements CommentAPI {

    private final CommentService commentService;
    // In a real app, this would come from the Spring Security context
    private static final String FAKE_AUTHENTICATED_USER_ID = "036dc698-3b84-49e1-8999-25e57bcb7a8a";

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Override
    public ResponseEntity<?> createComment(@Valid @RequestBody CommentCreateUpdate request) {
        var response = commentService.createComment(request, FAKE_AUTHENTICATED_USER_ID);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/comments/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<CommentResponse> getCommentById(String id) {
        var comment = commentService.getCommentById(id);
        return ResponseEntity.ok(CommentResponse.from(comment));
    }

    @Override
    public Pagination<CommentResponse> searchComments(int pageNumber, int pageSize, String sort,
                                                      String direction, Map<String, String> filters) {

        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = new SearchQuery(page, filters);

        return commentService.getAllComments(searchQuery)
                .map(CommentResponse::from);
    }

    @Override
    public Pagination<CommentResponse> getCommentsByTarget(TargetType targetType, String targetId, int pageNumber, int pageSize, String sort, String direction) {
        var page = Page.of(pageNumber, pageSize, sort, direction);
        return commentService.getAllComments(page, targetId, targetType).map(CommentResponse::from);
    }

    @Override
    public ResponseEntity<?> updateComment(String id, @Valid @RequestBody CommentCreateUpdate request) {
        commentService.updateComment(id, request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> deleteComment(String id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
