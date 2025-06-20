package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import com.sysm.devsync.infrastructure.repositories.WorkspaceJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

@Repository
public class WorkspacePersistence implements WorkspacePersistencePort {

    private static final Set<String> VALID_SEARCHABLE_FIELDS = Set.of(
            "name",
            "description",
            "isPrivate"
    );
    private final WorkspaceJpaRepository repository;

    public WorkspacePersistence(WorkspaceJpaRepository repository) {
        this.repository = repository;
    }

    private static boolean isValidSearchableField(String key) {
        return VALID_SEARCHABLE_FIELDS.contains(key);
    }

    @Override
    public void create(Workspace model) {
        if (model == null) {
            throw new BusinessException("Workspace model cannot be null");
        }
        var workspaceJpaEntity = WorkspaceJpaEntity.fromModel(model);
        repository.save(workspaceJpaEntity);
    }

    @Override
    public void update(Workspace model) {
        if (model == null) {
            throw new BusinessException("Workspace model cannot be null");
        }
        var workspaceJpaEntity = WorkspaceJpaEntity.fromModel(model);
        repository.save(workspaceJpaEntity);
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Workspace ID cannot be null or blank");
        }
        repository.deleteById(id);
    }

    @Override
    public Optional<Workspace> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Workspace ID cannot be null or blank");
        }
        return repository.findById(id)
                .map(WorkspaceJpaEntity::toModel);
    }

    @Override
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Workspace ID cannot be null or blank");
        }
        return repository.existsById(id);
    }

    @Override
    public Pagination<Workspace> findAll(SearchQuery searchQuery) {
        if (searchQuery == null) {
            throw new BusinessException("Search query cannot be null");
        }

        Specification<WorkspaceJpaEntity> spec = (root, query, criteriaBuilder) -> {
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
                page.map(WorkspaceJpaEntity::toModel).toList()
        );
    }
}
