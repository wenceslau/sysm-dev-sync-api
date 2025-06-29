package com.sysm.devsync.infrastructure.controllers.rest;

import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.infrastructure.controllers.dto.request.ProjectCreateUpdate;
import com.sysm.devsync.infrastructure.controllers.dto.response.ProjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/projects")
@Tag(name = "Projects")
public interface ProjectAPI {

    @PostMapping
    @Operation(summary = "Create a new project")
    @ApiResponse(responseCode = "201", description = "Project created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or workspace not found")
    ResponseEntity<?> createProject(@RequestBody ProjectCreateUpdate request);

    @GetMapping("/{id}")
    @Operation(summary = "Get a project by its ID")
    ResponseEntity<ProjectResponse> getProjectById(@PathVariable("id") String id);

    @GetMapping
    @Operation(summary = "Search for projects with pagination")
    Pagination<ProjectResponse> searchProjects(
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "direction", defaultValue = "asc") String direction,
            @RequestParam Map<String, String> filters
    );

    @PutMapping("/{id}")
    @Operation(summary = "Update a project's details")
    @ApiResponse(responseCode = "204", description = "Project updated successfully")
    @ApiResponse(responseCode = "404", description = "Project not found")
    ResponseEntity<?> updateProject(@PathVariable("id") String id, @RequestBody ProjectCreateUpdate request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    @ApiResponse(responseCode = "204", description = "Project deleted successfully")
    @ApiResponse(responseCode = "404", description = "Project not found")
    ResponseEntity<?> deleteProject(@PathVariable("id") String id);

    @PatchMapping("/{id}/workspace/{workspaceId}")
    @Operation(summary = "Change the workspace a project belongs to")
    @ApiResponse(responseCode = "204", description = "Workspace changed successfully")
    @ApiResponse(responseCode = "404", description = "Project or new Workspace not found")
    ResponseEntity<?> changeWorkspace(@PathVariable("id") String id, @PathVariable("workspaceId") String workspaceId);
}
