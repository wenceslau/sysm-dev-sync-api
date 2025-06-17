package com.sysm.devsync.infrastructure.repositories.tag;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Pagination;
import com.sysm.devsync.domain.SearchQuery;
import com.sysm.devsync.domain.models.Tag;
import com.sysm.devsync.domain.persistence.TagPersistencePort;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class TagPersistence implements TagPersistencePort {

    private static final Logger logger = LoggerFactory.getLogger(TagPersistence.class);
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
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void update(Tag model) {
        tagRepository.save(TagJpaEntity.fromModel(model));
    }

    @Transactional
    public void deleteById(String id) {
        tagRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Tag> findById(String id) {
        return tagRepository.findById(id)
                .map(TagJpaEntity::toModel);
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return tagRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Pagination<Tag> findAll(SearchQuery searchQuery) {

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

        var pageRequest = PageRequest.of(
                searchQuery.pageable().page(),
                searchQuery.pageable().perPage(),
                Sort.by(Sort.Direction.fromString(searchQuery.pageable().direction()), searchQuery.pageable().sort())
        );

        var page = tagRepository.findAll(spec, pageRequest);

        return new Pagination<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.map(TagJpaEntity::toModel).toList()
        );
    }

    private static HashMap<String, String> buildTerms(String terms) {

        var mapTerms = new HashMap<String, String>();
        if (terms == null || terms.trim().isEmpty()) {
            return mapTerms; // Return an empty map for null or empty input
        }

        var listTerms = Arrays.stream(terms.split("#"))
                .filter(term -> !term.trim().isEmpty()) // Avoid processing empty segments
                .toList();

        for (String term : listTerms) {
            if (term.contains("=")) {
                var split = term.split("=", 2); // <-- Changed to split on the first "=" only
                var key = split[0].trim();
                var value = split[1].trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    mapTerms.put(key, value);
                }
            }
        }
        return mapTerms;
    }

    private static boolean isValidSearchableField(String fieldName) {
        return VALID_SEARCHABLE_FIELDS.contains(fieldName);
    }
}
