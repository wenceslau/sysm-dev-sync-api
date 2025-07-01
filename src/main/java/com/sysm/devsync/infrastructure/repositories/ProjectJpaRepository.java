package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    Page<ProjectJpaEntity> findAll(Specification<ProjectJpaEntity> whereClause, Pageable page);

    boolean existsByWorkspaceId(String workspaceId);
}
