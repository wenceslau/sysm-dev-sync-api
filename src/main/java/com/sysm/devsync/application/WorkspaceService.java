package com.sysm.devsync.application;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.NotFoundException;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.infrastructure.controllers.dto.response.CreateResponse;
import com.sysm.devsync.infrastructure.controllers.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import com.sysm.devsync.infrastructure.controllers.dto.response.WorkspaceResponse;
import com.sysm.devsync.infrastructure.repositories.objects.KeyValue;

import java.util.List;
import java.util.Objects;

public class WorkspaceService {

    private final WorkspacePersistencePort workspacePersistence;
    private final UserPersistencePort userPersistence;
    private final ProjectPersistencePort projectPersistence; // <-- ADDED

    public WorkspaceService(WorkspacePersistencePort workspacePersistence, UserPersistencePort userPersistence, ProjectPersistencePort projectPersistence) {
        this.workspacePersistence = workspacePersistence;
        this.userPersistence = userPersistence;
        this.projectPersistence = projectPersistence;
    }

    public CreateResponse createWorkspace(WorkspaceCreateUpdate workspaceCreateUpdate, String ownerId) {

        var userExists = userPersistence.existsById(ownerId);
        if (!userExists) {
            throw new NotFoundException("User not found", ownerId);
        }
        Workspace workspace = Workspace.create(
                workspaceCreateUpdate.name(),
                workspaceCreateUpdate.description(),
                workspaceCreateUpdate.isPrivate(),
                ownerId
        );

        workspacePersistence.create(workspace);

        return new CreateResponse(workspace.getId());
    }

    public void updateWorkspace(String workspaceId, WorkspaceCreateUpdate workspaceUpdate) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));

        workspace.update(
                workspaceUpdate.name(),
                workspaceUpdate.description()
        );

        workspacePersistence.update(workspace);

        if (workspaceUpdate.isPrivate() != null && workspaceUpdate.isPrivate() != workspace.isPrivate()) {
            changeWorkspacePrivacy(workspaceId, workspaceUpdate.isPrivate());
        }
    }

    public void changeWorkspacePrivacy(String workspaceId, boolean isPrivate) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));

        workspace.setPrivate(isPrivate);
        workspacePersistence.update(workspace);
    }

    public void addMemberToWorkspace(String workspaceId, String memberId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));

        var exist = userPersistence.existsById(memberId);
        if (!exist) {
            throw new NotFoundException("Member not found", memberId);
        }

        workspace.addMember(memberId);

        workspacePersistence.update(workspace);
    }

    public void removeMemberFromWorkspace(String workspaceId, String memberId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));

        if (!workspace.getMembersId().contains(memberId)) {
            throw new NotFoundException("Member not found in workspace", memberId);
        }
        workspace.removeMember(memberId);

        workspacePersistence.update(workspace);
    }

    public void changeOwnerOfWorkspace(String workspaceId, String newOwnerId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));

        var exist = userPersistence.existsById(newOwnerId);
        if (!exist) {
            throw new NotFoundException("New owner not found", newOwnerId);
        }

        workspace.changeOwner(newOwnerId);
        workspacePersistence.update(workspace);
    }

    public void deleteWorkspace(String workspaceId) {
        // 1. First, ensure the workspace actually exists.
        if (!workspacePersistence.existsById(workspaceId)) {
            throw new NotFoundException("Workspace not found", workspaceId);
        }

        // 2. Enforce business rule: check for members.
        if (workspacePersistence.hasMembers(workspaceId)) {
            throw new BusinessException("Cannot delete a workspace that has members. Please remove all members first.");
        }

        // 3. Enforce business rule: check for projects.
        if (projectPersistence.existsByWorkspaceId(workspaceId)) {
            throw new BusinessException("Cannot delete a workspace that has associated projects. Please move or delete them first.");
        }

        workspacePersistence.deleteById(workspaceId);
    }

    public List<KeyValue> countProjects(List<String> workspaceIds) {
        return projectPersistence.countProjectsByWorkspaceIdIn(workspaceIds);
    }

    public Workspace getWorkspaceById(String workspaceId) {
        return workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found", workspaceId));
    }

    public Pagination<WorkspaceResponse> getAllWorkspaces(SearchQuery query) {
        var workspacePage = workspacePersistence.findAll(query);

        if (workspacePage.items().isEmpty()) {
            return workspacePage.map(ws -> WorkspaceResponse.from(ws, 0));
        }

        var workspaceIds = workspacePage.items().stream()
                .map(Workspace::getId)
                .toList();

        var ownerIds = workspacePage.items().stream()
                .map(Workspace::getOwnerId)
                .toList();

        var mapProjectCounts = projectPersistence.countProjectsByWorkspaceIdIn(workspaceIds);
        var mapUserNames = userPersistence.userIdXUseName(ownerIds);

        return workspacePage.map(ws -> {
            Object countValue = mapProjectCounts.stream()
                    .filter(x->x.key().equals(ws.getId()))
                    .findFirst()
                    .map(KeyValue::value)
                    .orElse(0L);

            Object nameValue = mapUserNames.stream()
                    .filter(x->x.key().equals(ws.getOwnerId()))
                    .findFirst()
                    .map(KeyValue::value)
                    .orElse("");

            var count = Long.parseLong(String.valueOf(countValue));
            var name =  String.valueOf(nameValue);

            return WorkspaceResponse.from(ws, count, name);
        });

    }
}
