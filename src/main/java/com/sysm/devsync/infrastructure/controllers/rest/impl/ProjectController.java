package com.sysm.devsync.infrastructure.controllers.rest.impl;

import com.sysm.devsync.application.ProjectService;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.infrastructure.controllers.dto.request.ProjectCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.ProjectResponse;
import com.sysm.devsync.infrastructure.controllers.rest.ProjectAPI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class ProjectController implements ProjectAPI {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<?> createProject(@Valid @RequestBody ProjectCreateUpdate request) {
        var response = projectService.createProject(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location)
                .body(response);
    }

    @Override
    public ResponseEntity<ProjectResponse> getProjectById(String id) {
        var project = projectService.getProjectById(id);
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    @Override
    public Pagination<ProjectResponse> searchProjects(int pageNumber, int pageSize, String sort, String direction, String terms) {
        var page = Page.of(pageNumber, pageSize, sort, direction);
        var searchQuery = new SearchQuery(page, terms);
        return projectService.getAllProjects(searchQuery)
                .map(ProjectResponse::from);
    }

    @Override
    public ResponseEntity<?> updateProject(String id, @Valid @RequestBody ProjectCreateUpdate request) {
        projectService.updateProject(id, request);
        return ResponseEntity.noContent()
                .build();
    }

    @Override
    public ResponseEntity<?> deleteProject(String id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent()
                .build();
    }

    @Override
    public ResponseEntity<?> changeWorkspace(String id, String workspaceId) {
        projectService.changeWorkspace(id, workspaceId);
        return ResponseEntity.noContent()
                .build();
    }
}
