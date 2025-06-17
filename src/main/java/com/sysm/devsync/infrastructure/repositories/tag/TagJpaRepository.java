package com.sysm.devsync.infrastructure.repositories.tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagJpaRepository extends JpaRepository<TagJpaEntity, String> {

    Page<TagJpaEntity> findAll(Specification<TagJpaEntity> whereClause, Pageable page);
}
