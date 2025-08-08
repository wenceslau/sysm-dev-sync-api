package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Project;
import com.sysm.devsync.domain.persistence.ProjectPersistencePort;
import com.sysm.devsync.infrastructure.repositories.ProjectJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.ProjectJpaEntity;
import com.sysm.devsync.infrastructure.repositories.objects.CountProject;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class ProjectPersistence extends AbstractPersistence<ProjectJpaEntity> implements ProjectPersistencePort {

    private final ProjectJpaRepository repository;

    public ProjectPersistence(ProjectJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Project model) {
        if (model == null) {
            throw new IllegalArgumentException("Project model cannot be null");
        }
        var entity = ProjectJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void update(Project model) {
        if (model == null) {
            throw new IllegalArgumentException("Project model cannot be null");
        }
        var entity = ProjectJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        return repository.findById(id)
                .map(ProjectJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Project ID cannot be null or empty");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Project> findAll(SearchQuery searchQuery) {
        Specification<ProjectJpaEntity> spec = buildSpecification(searchQuery);

        var pageRequest = buildPageRequest(searchQuery);
        var pageProject = repository.findAll(spec, pageRequest);

        return new Pagination<>(
                pageProject.getNumber(),
                pageProject.getSize(),
                pageProject.getTotalElements(),
                pageProject.map(ProjectJpaEntity::toModel).toList()
        );
    }

    @Transactional(readOnly = true)
    public boolean existsByWorkspaceId(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID cannot be null or blank");
        }
        return repository.existsByWorkspaceId(workspaceId);
    }

    @Override
    public List<CountProject> countProjectsByWorkspaceIdIn(List<String> workspaceIds) {
        return repository.countProjectsByWorkspaceIdIn(workspaceIds);
    }

    protected Predicate createPredicateForField(Root<ProjectJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "id", "name", "description" -> crBuilder.like(crBuilder.lower(root.get(key)), like(value));

            case "workspaceId" -> crBuilder.equal(root.get("workspace").get("id"), value);

            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
