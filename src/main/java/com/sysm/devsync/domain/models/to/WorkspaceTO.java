package com.sysm.devsync.domain.models.to;

public record WorkspaceTO(String id, String name) {

    public static WorkspaceTO of(String id, String name) {
        return new WorkspaceTO(id, name);
    }

    public static WorkspaceTO of(String id) {
        return new WorkspaceTO(id, null);
    }
}
