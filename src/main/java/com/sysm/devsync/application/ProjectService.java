package com.sysm.devsync.application;

import com.sysm.devsync.controller.dto.CreateResponse;
import com.sysm.devsync.controller.dto.request.ProjectCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.repositories.ProjectPersistencePort;
import com.sysm.devsync.domain.repositories.WorkspacePersistencePort;

public class ProjectService {

    private final ProjectPersistencePort projectPersistence;
    private final WorkspacePersistencePort workspacePersistence;

    public ProjectService(ProjectPersistencePort projectPersistence, WorkspacePersistencePort workspacePersistence) {
        this.projectPersistence = projectPersistence;
        this.workspacePersistence = workspacePersistence;
    }

    public CreateResponse createProject(ProjectCreateUpdate projectCreateUpdate) {

        var exist = workspacePersistence.existsById(projectCreateUpdate.workspaceId());
        if (!exist) {
            throw new IllegalArgumentException("Workspace not found");
        }

        var project = Project.create(
                projectCreateUpdate.name(),
                projectCreateUpdate.description(),
                projectCreateUpdate.workspaceId()
        );
        projectPersistence.create(project);
        return new CreateResponse(project.getId());
    }

    public void updateProject(String projectId, ProjectCreateUpdate projectUpdate) {
        var project = projectPersistence.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        project.update(
                projectUpdate.name(),
                projectUpdate.description()
        );

        projectPersistence.update(project);
    }

    public void changeWorkspace(String projectId, String workspaceId) {

        var project = projectPersistence.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        var exist = workspacePersistence.existsById(workspaceId);
        if (!exist) {
            throw new IllegalArgumentException("Workspace not found");
        }

        project.changeWorkspace(workspaceId);
        projectPersistence.update(project);
    }

    public void deleteProject(String projectId) {
        projectPersistence.deleteById(projectId);
    }

    public Project getProjectById(String projectId) {
        return projectPersistence.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    }

    public Pagination<Project> getAllProjects(SearchQuery query) {
        return projectPersistence.findAll(query);
    }

}
