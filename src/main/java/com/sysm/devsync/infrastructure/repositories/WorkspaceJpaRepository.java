package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, String> {

    Page<WorkspaceJpaEntity> findAll(Specification<WorkspaceJpaEntity> whereClause, Pageable page);

    @Query("SELECT count(m) > 0 FROM Workspace w JOIN w.members m WHERE w.id = :workspaceId")
    boolean hasMembers(@Param("workspaceId") String workspaceId);

}
