package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Workspace;
import com.sysm.devsync.domain.persistence.WorkspacePersistencePort;
import com.sysm.devsync.infrastructure.repositories.WorkspaceJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.WorkspaceJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class WorkspacePersistence extends AbstractPersistence<WorkspaceJpaEntity> implements WorkspacePersistencePort {

    private final WorkspaceJpaRepository repository;

    public WorkspacePersistence(WorkspaceJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Workspace model) {
        if (model == null) {
            throw new IllegalArgumentException("Workspace model cannot be null");
        }
        var workspaceJpaEntity = WorkspaceJpaEntity.fromModel(model);
        repository.save(workspaceJpaEntity);
    }

    @Transactional
    public void update(Workspace model) {
        if (model == null) {
            throw new IllegalArgumentException("Workspace model cannot be null");
        }
        var workspaceJpaEntity = WorkspaceJpaEntity.fromModel(model);
        repository.save(workspaceJpaEntity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Workspace ID cannot be null or blank");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Workspace> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Workspace ID cannot be null or blank");
        }
        return repository.findById(id)
                .map(WorkspaceJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Workspace ID cannot be null or blank");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Workspace> findAll(SearchQuery searchQuery) {
        Specification<WorkspaceJpaEntity> spec = buildSpecification(searchQuery);

        var pageRequest = buildPageRequest(searchQuery);
        var page = repository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(WorkspaceJpaEntity::toModel).toList()
        );
    }

    @Transactional(readOnly = true)
    public boolean hasMembers(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID cannot be null or blank");
        }
        return repository.hasMembers(workspaceId);
    }

    protected Predicate createPredicateForField(Root<WorkspaceJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {

        return switch (key) {
            case "id" -> crBuilder.equal(root.get("id"), value);
            case "name", "description" -> crBuilder.like(crBuilder.lower(root.get(key)), like(value));
            case "isPrivate" -> {
                if ("true".equalsIgnoreCase(value)) {
                    yield crBuilder.isTrue(root.get("isPrivate"));
                } else if ("false".equalsIgnoreCase(value)) {
                    yield crBuilder.isFalse(root.get("isPrivate"));
                } else {
                    throw new BusinessException("Invalid value for boolean field '" + key + "': '" + value + "'. Expected 'true' or 'false'.");
                }
            }
            case "ownerId" -> crBuilder.equal(root.get("owner").get("id"), value);
            case "ownerName" -> crBuilder.like(crBuilder.lower(root.join("owner").get("name")), like(value));
            case "memberId" -> crBuilder.equal(root.join("members").get("id"), value);
            case "memberName" -> crBuilder.like(crBuilder.lower(root.join("members").get("name")), like(value));
            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
