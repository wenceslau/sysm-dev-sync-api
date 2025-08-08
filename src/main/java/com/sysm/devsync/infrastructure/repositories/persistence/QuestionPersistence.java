package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.QuestionStatus;
import com.sysm.devsync.domain.models.Question;
import com.sysm.devsync.domain.persistence.QuestionPersistencePort;
import com.sysm.devsync.infrastructure.repositories.QuestionJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.QuestionJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class QuestionPersistence extends AbstractPersistence<QuestionJpaEntity> implements QuestionPersistencePort {

    private final QuestionJpaRepository repository;

    public QuestionPersistence(QuestionJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Question model) {
        if (model == null) {
            throw new IllegalArgumentException("Question model must not be null");
        }
        QuestionJpaEntity entity = QuestionJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void update(Question model) {
        if (model == null) {
            throw new IllegalArgumentException("Question model must not be null");
        }
        QuestionJpaEntity entity = QuestionJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Question ID must not be null or empty");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Question> findById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Question ID must not be null or empty");
        }
        return repository.findById(id)
                .map(QuestionJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Question ID must not be null or empty");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Question> findAll(SearchQuery query) {
        var pageableRequest = buildPageRequest(query);
        var specification = buildSpecification(query);

        var questionPage = repository.findAll(specification, pageableRequest);

        return new Pagination<>(
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.map(QuestionJpaEntity::toModel).toList()
        );
    }

    @Transactional
    public Pagination<Question> findAllByProjectId(Page page, String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            throw new IllegalArgumentException("Project ID must not be null or empty");
        }

        var pageableRequest = buildPageRequest(page);
        var questionPage = repository.findAllByProject_Id(projectId, pageableRequest);

        return new Pagination<>(
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.map(QuestionJpaEntity::toModel).toList()
        );
    }

    protected Predicate createPredicateForField(Root<QuestionJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "id" -> crBuilder.equal(root.get("id"), value);
            case "title" -> crBuilder.like(crBuilder.lower(root.get("title")), like(value));
            case "description" -> crBuilder.like(crBuilder.lower(root.get("description")), like(value));
            case "projectId" -> crBuilder.equal(root.get("project").get("id"), value);
            case "authorId" -> crBuilder.equal(root.get("author").get("id"), value);
            case "status" -> crBuilder.equal(root.get("status"), QuestionStatus.valueOf(value));
            case "tagsId" -> crBuilder.equal(root.join("tags").get("id"), value);
            case "tagsName" -> crBuilder.equal(root.join("tags").get("name"), value);
            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
