package com.sysm.devsync.domain;

import java.util.Set;

public interface DomainRepository<T> {

    void create(T model);

    void update(T model);

    void deleteById(String id);

    T findById(String id);

    Set<T> findAll(SearchQuery query);

}
