package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.infrastructure.repositories.objects.KeyValue;

import java.util.List;

public interface ProjectPersistencePort extends PersistencePort<Project> {

    boolean existsByWorkspaceId(String workspaceId);

    List<KeyValue> countProjectsByWorkspaceIdIn(List<String> workspaceIds);
}
