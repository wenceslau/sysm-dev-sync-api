package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.infrastructure.repositories.entities.UserJpaEntity;
import com.sysm.devsync.infrastructure.repositories.UserJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserPersistence implements UserPersistencePort {

    private static final Set<String> VALID_SEARCHABLE_FIELDS = Set.of(
            "name",
            "email",
            "role"
    );
    private final UserJpaRepository repository;

    public UserPersistence(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(User model) {
        if (model == null) {
            throw new BusinessException("User model cannot be null");
        }
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void update(User model) {
        if (model == null) {
            throw new BusinessException("User model cannot be null");
        }
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("User ID cannot be null or blank");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("User ID cannot be null or blank");
        }
        return repository.findById(id)
                .map(UserJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("User ID cannot be null or blank");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<User> findAll(SearchQuery searchQuery) {
        if (searchQuery == null) {
            throw new BusinessException("Search query cannot be null");
        }

        Specification<UserJpaEntity> spec = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            var mapTerms = buildTerms(searchQuery.terms());

            mapTerms.forEach((key, value) -> {
                if (!isValidSearchableField(key)) { // Validate the field name
                    throw new BusinessException("Invalid search field provided: '" + key + "'");
                }
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
            });

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();  // Represents a TRUE predicate (matches all)
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };

        var pageRequest = buildPageRequest(searchQuery);
        var page = repository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(UserJpaEntity::toModel).toList()
        );

    }

    private static boolean isValidSearchableField(String key) {
        return VALID_SEARCHABLE_FIELDS.contains(key);
    }
}
