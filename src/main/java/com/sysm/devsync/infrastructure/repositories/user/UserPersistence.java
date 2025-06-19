package com.sysm.devsync.infrastructure.repositories.user;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.User;
import com.sysm.devsync.domain.persistence.UserPersistencePort;
import com.sysm.devsync.infrastructure.repositories.tag.TagJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void update(User model) {
        var userJpaEntity = UserJpaEntity.fromModel(model);
        repository.save(userJpaEntity);
    }

    @Transactional
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return repository.findById(id)
                .map(UserJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<User> findAll(SearchQuery searchQuery) {

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

        var pageRequest = PageRequest.of(
                searchQuery.pageable().page(),
                searchQuery.pageable().perPage(),
                Sort.by(Sort.Direction.fromString(searchQuery.pageable().direction()), searchQuery.pageable().sort())
        );
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
