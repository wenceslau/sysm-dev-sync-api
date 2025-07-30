package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.WorkspaceService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.infrastructure.controllers.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.WorkspaceResponse;
import com.sysm.devsync.infrastructure.controllers.rest.WorkspaceAPI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
public class WorkspaceController extends AbstractController implements WorkspaceAPI {

    private final WorkspaceService workspaceService;

    // For now, we'll hardcode the ownerId. In a real app, this would come from security context.

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Override
    public ResponseEntity<?> create(@Valid @RequestBody WorkspaceCreateUpdate request) {
        var response = workspaceService.createWorkspace(request, authenticatedUserId());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Override
    public ResponseEntity<WorkspaceResponse> getById(String id) {
        var workspace = workspaceService.getWorkspaceById(id);
        return ResponseEntity.ok(WorkspaceResponse.from(workspace));
    }

    @Override
    public Pagination<WorkspaceResponse> search(int pageNumber, int pageSize, String sort,
                                                String direction, Map<String, String> filters) {

        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = SearchQuery.of(page, filters);

        return workspaceService.getAllWorkspaces(searchQuery).map(WorkspaceResponse::from);
    }

    @Override
    public ResponseEntity<?> update(String id, @Valid @RequestBody WorkspaceCreateUpdate request) {
        workspaceService.updateWorkspace(id, request);
        return ResponseEntity.noContent().build();
    }


    @Override
    public ResponseEntity<?> delete(String id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> changePrivacy(String id, boolean isPrivate) {
        workspaceService.changeWorkspacePrivacy(id, isPrivate);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> addMember(String id, String memberId) {
        workspaceService.addMemberToWorkspace(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> removeMember(String id, String memberId) {
        workspaceService.removeMemberFromWorkspace(id, memberId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> changeOwner(String id, String newOwnerId) {
        workspaceService.changeOwnerOfWorkspace(id, newOwnerId);
        return ResponseEntity.noContent().build();
    }
}
