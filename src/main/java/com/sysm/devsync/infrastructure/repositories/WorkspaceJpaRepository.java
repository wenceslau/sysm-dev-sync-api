package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, String> {

    Page<WorkspaceJpaEntity> findAll(Specification<WorkspaceJpaEntity> whereClause, Pageable page);
}
