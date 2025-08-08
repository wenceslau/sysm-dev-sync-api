package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Answer;
import com.sysm.devsync.domain.persistence.AnswerPersistencePort;
import com.sysm.devsync.infrastructure.repositories.AnswerJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.AnswerJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class AnswerPersistence extends AbstractPersistence<AnswerJpaEntity> implements AnswerPersistencePort {

    private final AnswerJpaRepository repository;

    public AnswerPersistence(AnswerJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Answer model) {
        if (model == null) {
            throw new IllegalArgumentException("Answer model cannot be null");
        }
        var entity = AnswerJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void update(Answer model) {
        if (model == null) {
            throw new IllegalArgumentException("Answer model cannot be null");
        }
        var entity = AnswerJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Answer ID cannot be null or blank");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Answer> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Answer ID cannot be null or blank");
        }
        return repository.findById(id)
                .map(AnswerJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Answer ID cannot be null or blank");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Answer> findAll(SearchQuery searchQuery) {
        Specification<AnswerJpaEntity> spec = buildSpecification(searchQuery);

        var pageRequest = buildPageRequest(searchQuery);
        var page = repository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(AnswerJpaEntity::toModel).toList()
        );
    }

    @Transactional(readOnly = true)
    public Pagination<Answer> findAllByQuestionId(Page page, String questionId) {
        if (questionId == null || questionId.isEmpty()) {
            throw new IllegalArgumentException("Project ID must not be null or empty");
        }

        var pageableRequest = buildPageRequest(page);
        var questionPage = repository.findAllByQuestion_Id(questionId, pageableRequest);

        return new Pagination<>(
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.map(AnswerJpaEntity::toModel).toList()
        );
    }

    @Override
    public void deleteAllByQuestionId(String questionId) {
        if (questionId == null){
            throw new IllegalArgumentException("Question ID cannot be null");
        }
        repository.deleteAllByQuestion_Id(questionId);
    }

    protected Predicate createPredicateForField(Root<AnswerJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key){
            case "id" -> crBuilder.equal(root.get("id"), value);
            case "content" -> crBuilder.like(crBuilder.lower(root.get("content")), like(value));
            case "isAccepted" -> {
                if ("true".equalsIgnoreCase(value)) {
                    yield crBuilder.isTrue(root.get("isAccepted"));
                } else if ("false".equalsIgnoreCase(value)) {
                    yield crBuilder.isFalse(root.get("isAccepted"));
                } else {
                    throw new BusinessException("Invalid value for isAccepted field: invalid. Expected 'true' or 'false'. " + value);
                }
            }
            case "authorId" -> crBuilder.equal(root.get("author").get("id"), value);
            case "authorName" -> crBuilder.like(crBuilder.lower(root.join("author").get("name")), like(value));
            case "questionId" -> crBuilder.equal(root.get("question").get("id"), value);
            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
