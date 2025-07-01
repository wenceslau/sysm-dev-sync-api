package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.enums.TargetType;
import com.sysm.devsync.domain.models.Comment;
import com.sysm.devsync.domain.persistence.CommentPersistencePort;
import com.sysm.devsync.infrastructure.repositories.CommentJpaRepository;
import com.sysm.devsync.infrastructure.repositories.entities.CommentJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class CommentPersistence extends AbstractPersistence<CommentJpaEntity> implements CommentPersistencePort {

    private final CommentJpaRepository repository;

    public CommentPersistence(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void create(Comment model) {
        if (model == null) {
            throw new IllegalArgumentException("Comment model must not be null");
        }
        CommentJpaEntity entity = CommentJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void update(Comment model) {
        if (model == null) {
            throw new IllegalArgumentException("Comment model must not be null");
        }
        CommentJpaEntity entity = CommentJpaEntity.fromModel(model);
        repository.save(entity);
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Comment ID must not be null or empty");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Comment> findById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Comment ID must not be null or empty");
        }
        return repository.findById(id)
                .map(CommentJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Comment ID must not be null or empty");
        }
        return repository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Comment> findAll(SearchQuery query) {
        var pageableRequest = buildPageRequest(query);
        var specification = buildSpecification(query);

        var questionPage = repository.findAll(specification, pageableRequest);

        return new Pagination<>(
                questionPage.getNumber(),
                questionPage.getSize(),
                questionPage.getTotalElements(),
                questionPage.map(CommentJpaEntity::toModel).toList()
        );
    }

    @Transactional(readOnly = true)
    public Pagination<Comment> findAllByTargetId(Page page, TargetType targetType, String targetId) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        if (targetId == null || targetId.isEmpty()) {
            throw new IllegalArgumentException("Target ID must not be null or empty");
        }

        var pageableRequest = buildPageRequest(page);
        var notePage = repository.findAllByTargetTypeAndTargetId(targetType, targetId, pageableRequest);

        return new Pagination<>(
                notePage.getNumber(),
                notePage.getSize(),
                notePage.getTotalElements(),
                notePage.map(CommentJpaEntity::toModel).toList()
        );
    }

    @Override
    @Transactional
    public void deleteAllByTargetTypeAndTargetId(TargetType targetType, String targetId) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        if (targetId == null || targetId.isEmpty()) {
            throw new IllegalArgumentException("Target ID must not be null or empty");
        }
        repository.deleteAllByTargetTypeAndTargetId(targetType, targetId);
    }

    protected Predicate createPredicateForField(Root<CommentJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "targetType" -> crBuilder.equal(root.get("targetType"), TargetType.valueOf(value));
            case "targetId" -> crBuilder.equal(root.get("targetId"), value);
            case "content" -> crBuilder.like(crBuilder.lower(root.get("content")), like(value));
            case "authorId" -> crBuilder.equal(root.get("author").get("id"), value);
            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
