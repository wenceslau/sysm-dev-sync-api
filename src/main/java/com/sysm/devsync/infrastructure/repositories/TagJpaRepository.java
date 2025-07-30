package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagJpaRepository extends JpaRepository<TagJpaEntity, String> {

    Page<TagJpaEntity> findAll(Specification<TagJpaEntity> whereClause, Pageable page);

    @Modifying
    @Query("UPDATE Tag t SET t.amountUsed = t.amountUsed + 1 WHERE t.id = :id")
    void incrementUse(String id);

    @Modifying
    @Query("UPDATE Tag t SET t.amountUsed = t.amountUsed - 1 WHERE t.id = :id")
    void decrementUse(String id);
}
