package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.infrastructure.repositories.entities.CommentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, String> {

    Page<CommentJpaEntity> findAll(Specification<CommentJpaEntity> whereClause, Pageable page);

    Page<CommentJpaEntity> findAllByTargetTypeAndTargetId(
            TargetType targetType, String targetId, Pageable pageable
    );

    void deleteAllByTargetTypeAndTargetId(TargetType targetType, String targetId);

}
