package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.NoteJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteJpaRepository extends JpaRepository<NoteJpaEntity, String> {

    Page<NoteJpaEntity> findAll(Specification<NoteJpaEntity> whereClause, Pageable page);

    Page<NoteJpaEntity> findAllByProject_Id(String projectId, Pageable page);

}
