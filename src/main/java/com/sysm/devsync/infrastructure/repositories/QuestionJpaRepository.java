package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.QuestionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, String> {

    Page<QuestionJpaEntity> findAll(Specification<QuestionJpaEntity> whereClause, Pageable page);

    Page<QuestionJpaEntity> findAllByProject_Id(String projectId, Pageable page);

}
