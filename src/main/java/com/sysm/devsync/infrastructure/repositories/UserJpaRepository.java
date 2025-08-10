package com.sysm.devsync.infrastructure.repositories;

import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.objects.KeyValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Page<UserJpaEntity> findAll(Specification<UserJpaEntity> whereClause, Pageable page);

    @Query("SELECT new com.sysm.devsync.infrastructure.repositories.objects.KeyValue(u.id, u.name) " +
           "FROM User u " +
           "WHERE u.id IN :userIds ")
    List<KeyValue> userIdXUseName(List<String> userIds);

}
