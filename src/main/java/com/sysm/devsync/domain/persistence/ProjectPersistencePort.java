package com.sysm.devsync.domain.persistence;

import com.sysm.devsync.domain.PersistencePort;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.infrastructure.repositories.objects.CountProject;

import java.util.List;
import java.util.Map;

public interface ProjectPersistencePort extends PersistencePort<Project> {

    boolean existsByWorkspaceId(String workspaceId);

    List<CountProject> countProjectsByWorkspaceIdIn(List<String> workspaceIds);
}
