package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Note;
import com.sysm.devsync.domain.persistence.NotePersistencePort;
import com.sysm.devsync.infrastructure.repositories.NoteJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.NoteJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class NotePersistence extends AbstractPersistence<NoteJpaEntity> implements NotePersistencePort {

    private final NoteJpaRepository repository;

    public NotePersistence(NoteJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Note model) {
        if (model == null) {
            throw new IllegalArgumentException("Note model must not be null");
        }
        NoteJpaEntity entity = NoteJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void update(Note model) {
        if (model == null) {
            throw new IllegalArgumentException("Note model must not be null");
        }
        NoteJpaEntity entity = NoteJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Note ID must not be null or empty");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Note> findById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Note ID must not be null or empty");
        }
        return repository.findById(id)
                .map(NoteJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Note ID must not be null or empty");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Note> findAll(SearchQuery query) {
        var pageableRequest = buildPageRequest(query);
        var specification = buildSpecification(query);

        var questionPage = repository.findAll(specification, pageableRequest);

        return new Pagination<>(
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.map(NoteJpaEntity::toModel).toList()
        );
    }

    @Transactional(readOnly = true)
    public Pagination<Note> findAllByProjectId(Page page, String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Note ID must not be null or empty");
        }

        var pageableRequest = buildPageRequest(page);
        var notePage = repository.findAllByProject_Id(projectId, pageableRequest);

        return new Pagination<>(
                notePage.getNumber(),
                notePage.getSize(),
                notePage.getTotalElements(),
                notePage.map(NoteJpaEntity::toModel).toList()
        );
    }

    protected Predicate createPredicateForField(Root<NoteJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "title" -> crBuilder.like(crBuilder.lower(root.get("title")), like(value));
            case "content" -> crBuilder.like(crBuilder.lower(root.get("content")), like(value));
            case "authorId" -> crBuilder.equal(root.get("author").get("id"), value);
            case "projectId" -> crBuilder.equal(root.get("project").get("id"), value);
            case "version" -> {
                try {
                    yield crBuilder.equal(root.get("version"), Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new BusinessException("Invalid value for version field: '" + value + "'. Expected an integer.");
                }
            }
            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
