package com.sysm.devsync.infrastructure.repositories.persistence;

import com.sysm.devsync.domain.BusinessException;
import com.sysm.devsync.domain.Page;
import com.sysm.devsync.domain.SearchQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.sysm.devsync.infrastructure.Utils.like;

public abstract class AbstractPersistence<T> {

    protected HashMap<String, String> buildTerms(String terms) {

        var mapTerms = new HashMap<String, String>();
        if (terms != null && !terms.trim().isEmpty()) {
            var termsList = terms.split("#");

            for (String term : termsList) {
                String trimmedTerm = term.trim();

                if (!trimmedTerm.contains("=")) {
                    continue;
                }
                var split = trimmedTerm.split("=", 2);
                var key = split[0].trim();
                var value = split[1].trim();

                if (!key.isEmpty() && !value.isEmpty()) {
                    mapTerms.put(key, value);
                }
            }
        }
        return mapTerms;
    }

    protected PageRequest buildPageRequest(Page page) {
        if (page == null) {
            return PageRequest.of(
                    0,
                    10
            );
        }

        if (!StringUtils.hasText(page.direction()) && !StringUtils.hasText(page.sort())) {
            return PageRequest.of(
                    page.pageNumber(),
                    page.pageSize()
            );

        }

        return PageRequest.of(
                page.pageNumber(),
                page.pageSize(),
                Sort.by(
                        Sort.Direction.fromString(page.direction()),
                        page.sort()
                )
        );
    }

    protected PageRequest buildPageRequest(SearchQuery searchQuery) {
        if (searchQuery == null || searchQuery.page() == null) {
            return PageRequest.of(
                    0,
                    10
            );
        }

        // Ensure that the pageable object has a valid direction field
        if (!StringUtils.hasText(searchQuery.page().direction())) {
            return PageRequest.of(
                    searchQuery.page().pageNumber(),
                    searchQuery.page().pageSize()
            );
        }

        // Ensure a sort field is also present if a direction is provided
        if (!StringUtils.hasText(searchQuery.page().sort())) {
            return PageRequest.of(
                    searchQuery.page().pageNumber(),
                    searchQuery.page().pageSize()
            );
        }

        return PageRequest.of(
                searchQuery.page().pageNumber(),
                searchQuery.page().pageSize(),
                Sort.by(
                        Sort.Direction.fromString(searchQuery.page().direction()),
                        searchQuery.page().sort()
                )
        );
    }

    protected Specification<T> buildSpecification(SearchQuery searchQuery) {

        Specification<T> spec = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();
            var mapTerms = buildTerms(searchQuery.terms());

            mapTerms.forEach((key, value) -> {
                // Delegate predicate creation to the concrete subclass
                Predicate fieldPredicate = createPredicateForField(root, criteriaBuilder, key, value);
                if (fieldPredicate != null) { // Ensure the subclass returned a predicate
                    predicates.add(fieldPredicate);
                }
            });

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();  // Represents a TRUE predicate (matches all)
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };

        return spec;
    }

    protected abstract Predicate createPredicateForField(Root<T> root, CriteriaBuilder crBuilder, String key, String value);

    /**
     * Default implementation for creating predicates based on field types.
     * This method can be overridden by subclasses to provide custom logic.
     */
    private Predicate samplePredicateTypes(Root<T> root, CriteriaBuilder crBuilder, String key, String value) {
        return switch (key) {
            case "string" -> crBuilder.like(crBuilder.lower(root.get(key)), like(value));
            case "boolean" -> {
                if ("true".equalsIgnoreCase(value)) {
                    yield crBuilder.isTrue(root.get("isPrivate"));
                } else if ("false".equalsIgnoreCase(value)) {
                    yield crBuilder.isFalse(root.get("isPrivate"));
                } else {
                    throw new BusinessException("Invalid value for boolean field '" + key + "': '" + value + "'. Expected 'true' or 'false'.");
                }
            }
            case "manyToOneId" -> crBuilder.equal(root.get("owner").get("id"), value);
            case "manyToOneName" -> crBuilder.like(crBuilder.lower(root.join("owner").get("name")), like(value));
            case "manyToManyId" -> crBuilder.equal(root.join("members").get("id"), value);
            case "manyToManyName" -> crBuilder.like(crBuilder.lower(root.join("members").get("name")), like(value));
            case "localDateTimeField" -> {
                try {
                    yield crBuilder.equal(root.get(key), LocalDateTime.parse(value)); // Use the appropriate parser and format
                    // crBuilder.greaterThanOrEqualTo(root.get(key), LocalDateTime.parse(value));
                } catch (DateTimeParseException e) {
                    throw new BusinessException("Invalid date format for field '" + key + "': '" + value + "'");
                }
            }
            case "numberField" -> {
                try {
                    yield  crBuilder.equal(root.get(key), Integer.parseInt(value));
                    //crBuilder.greaterThan(root.get(key), Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new BusinessException("Invalid number format for field '" + key + "': '" + value + "'");
                }
            }
            default -> throw new BusinessException("Unsupported search logic for field: '" + key + "'");
        };
    }

}
