package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import com.sysm.devsync.infrastructure.repositories.TagJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.sysm.devsync.infrastructure.Utils.like;

@Repository
public class TagPersistence extends AbstractPersistence<TagJpaEntity> implements TagPersistencePort {

    private final TagJpaRepository tagRepository;

    public TagPersistence(TagJpaRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void create(Tag model) {
        if (model == null) {
            throw new IllegalArgumentException("Tag model cannot be null");
        }
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void update(Tag model) {
        if (model == null) {
            throw new IllegalArgumentException("Tag model cannot be null");
        }
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tag ID cannot be null or blank");
        }
        tagRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Tag> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tag ID cannot be null or blank");
        }
        return tagRepository.findById(id)
                .map(TagJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Tag ID cannot be null or blank");
        }
        return tagRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Tag> findAll(SearchQuery searchQuery) {
        Specification<TagJpaEntity> spec = buildSpecification(searchQuery);

        var pageRequest = buildPageRequest(searchQuery);

        var page = tagRepository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(TagJpaEntity::toModel).toList()
        );
    }

    protected Predicate createPredicateForField(Root<TagJpaEntity> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "name",
                 "color",
                 "description",
                 "category"  -> crBuilder.like(crBuilder.lower(root.get(key)), like(value));

            default -> throw new BusinessException("Invalid search field provided: '" + key + "'");
        };
    }
}
