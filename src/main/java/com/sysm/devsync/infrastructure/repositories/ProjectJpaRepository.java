package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.objects.CountProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    Page<ProjectJpaEntity> findAll(Specification<ProjectJpaEntity> whereClause, Pageable page);

    boolean existsByWorkspaceId(String workspaceId);

    int countByWorkspaceId(String workspaceId);

    @Query("SELECT new com.sysm.devsync.infrastructure.repositories.objects.CountProject(p.workspace.id, count(p.id)) " +
           "FROM Project p " +
           "WHERE p.workspace.id IN :workspaceIds " +
           "GROUP BY p.workspace.id")
    List<CountProject> countProjectsByWorkspaceIdIn(List<String> workspaceIds);
}
