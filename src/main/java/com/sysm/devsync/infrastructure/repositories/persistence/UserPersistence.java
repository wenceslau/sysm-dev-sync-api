package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class UserPersistence extends AbstractPersistence<UserJpaEntity> implements UserPersistencePort {

    private final UserJpaRepository repository;

    public UserPersistence(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(User model) {
        if (model == null) {
            throw new IllegalArgumentException("User model cannot be null");
        }
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void update(User model) {
        if (model == null) {
            throw new IllegalArgumentException("User model cannot be null");
        }
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        return repository.findById(id)
                .map(UserJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<User> findAll(SearchQuery searchQuery) {
        Specification<UserJpaEntity> spec = buildSpecification(searchQuery);
        var pageRequest = buildPageRequest(searchQuery);

        var page = repository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(UserJpaEntity::toModel).toList()
        );

    }

    protected Predicate createPredicateForField(Root<UserJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "name", "email" -> crBuilder.like(crBuilder.lower(root.get(key)), like(value));

            case "role" -> crBuilder.equal(crBuilder.lower(root.get(key)), value.toLowerCase());

            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }

}
