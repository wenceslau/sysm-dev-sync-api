package com.sysm.devsync.infrastructure.repositories.user;

import com.sysm.devsync.infrastructure.repositories.tag.TagJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Page<UserJpaEntity> findAll(Specification<UserJpaEntity> whereClause, Pageable page);
}
