package com.sysm.devsync.application;

import com.sysm.devsync.infrastructure.controller.dto.CreateResponse;
import com.sysm.devsync.infrastructure.controller.dto.request.WorkspaceCreateUpdate;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;

public class WorkspaceService {

    private final WorkspacePersistencePort workspacePersistence;
    private final UserPersistencePort userPersistence;

    public WorkspaceService(WorkspacePersistencePort workspacePersistence, UserPersistencePort userPersistence) {
        this.workspacePersistence = workspacePersistence;
        this.userPersistence = userPersistence;
    }

    public CreateResponse createWorkspace(WorkspaceCreateUpdate workspaceCreateUpdate, String ownerId) {
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
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        workspace.update(
                workspaceUpdate.name(),
                workspaceUpdate.description()
        );

        workspacePersistence.update(workspace);
    }

    public void changeWorkspacePrivacy(String workspaceId, boolean isPrivate) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        workspace.setPrivate(isPrivate);
        workspacePersistence.update(workspace);
    }

    public void addMemberToWorkspace(String workspaceId, String memberId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        var exist = userPersistence.existsById(memberId);
        if (!exist){
            throw new IllegalArgumentException("Member not found");
        }

        workspace.addMember(memberId);

        workspacePersistence.update(workspace);
    }

    public void removeMemberFromWorkspace(String workspaceId, String memberId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        if (!workspace.getMembersId().contains(memberId)) {
            throw new IllegalArgumentException("Member not found in workspace");
        }
        workspace.getMembersId().remove(memberId);

        workspacePersistence.update(workspace);
    }

    public void changeOwnerOfWorkspace(String workspaceId, String newOwnerId) {
        Workspace workspace = workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        var exist = userPersistence.existsById(newOwnerId);
        if (!exist){
            throw new IllegalArgumentException("New owner not found");
        }

        workspace.changeOwner(newOwnerId);
        workspacePersistence.update(workspace);
    }

    public void deleteWorkspace(String workspaceId) {
        workspacePersistence.deleteById(workspaceId);
    }

    public Workspace getWorkspaceById(String workspaceId) {
        return workspacePersistence.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    public Pagination<Workspace> getAllWorkspaces(SearchQuery query) {
        return workspacePersistence.findAll(query);
    }

}
