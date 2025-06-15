package com.sysm.devsync.domain;

import java.util.Optional;

public interface PersistencePort<T> {

    void create(T model);

    void update(T model);

    void deleteById(String id);

    Optional<T> findById(String id);

    boolean existsById(String id);

    Pagination<T> findAll(SearchQuery query);

}
