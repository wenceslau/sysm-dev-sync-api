package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import com.sysm.devsync.infrastructure.repositories.entities.TagJpaEntity;
import com.sysm.devsync.infrastructure.repositories.TagJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class TagPersistence extends AbstractPersistence implements TagPersistencePort {

    private static final Set<String> VALID_SEARCHABLE_FIELDS = Set.of(
            "name",
            "color",
            "description",
            "category"
    );
    private final TagJpaRepository tagRepository;

    public TagPersistence(TagJpaRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void create(Tag model) {
        if (model == null) {
            throw new BusinessException("Tag model cannot be null");
        }
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void update(Tag model) {
        if (model == null) {
            throw new BusinessException("Tag model cannot be null");
        }
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void deleteById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Tag ID cannot be null or blank");
        }
        tagRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Tag> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Tag ID cannot be null or blank");
        }
        return tagRepository.findById(id)
                .map(TagJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        if (id == null || id.isBlank()) {
            throw new BusinessException("Tag ID cannot be null or blank");
        }
        return tagRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Tag> findAll(SearchQuery searchQuery) {
        if (searchQuery == null) {
            throw new BusinessException("Search query cannot be null");
        }

        Specification<TagJpaEntity> spec = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            var mapTerms = buildTerms(searchQuery.terms());

            mapTerms.forEach((key, value) -> {
                if (!isValidSearchableField(key)) { // Validate the field name
                    throw new BusinessException("Invalid search field provided: '" + key + "'. This field will be ignored.");
                }
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
            });

            // Only apply 'or' if there are predicates
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();  // Represents a TRUE predicate (matches all)
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };

        var pageRequest = buildPageRequest(searchQuery);

        var page = tagRepository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(TagJpaEntity::toModel).toList()
        );
    }

    private static boolean isValidSearchableField(String fieldName) {
        return VALID_SEARCHABLE_FIELDS.contains(fieldName);
    }
}
