package com.sysm.devsync.domain;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public interface PersistencePort<T> {

    void create(T model);

    void update(T model);

    void deleteById(String id);

    Optional<T> findById(String id);

    boolean existsById(String id);

    Pagination<T> findAll(SearchQuery query);

    default HashMap<String, String> buildTerms(String terms) {

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

    default PageRequest buildPageRequest(SearchQuery searchQuery) {
        if (searchQuery == null || searchQuery.pageable() == null) {
            return PageRequest.of(
                    0,
                    10
            );
        }

        if (!StringUtils.hasText(searchQuery.pageable().direction())) {
            return  PageRequest.of(
                    searchQuery.pageable().page(),
                    searchQuery.pageable().perPage()
            );
        }

        return PageRequest.of(
                searchQuery.pageable().page(),
                searchQuery.pageable().perPage(),
                Sort.by(
                        Sort.Direction.fromString(searchQuery.pageable().direction()),
                        searchQuery.pageable().sort()
                )
        );
    }

}
