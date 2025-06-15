package com.sysm.devsync.domain;

import java.util.List;
import java.util.function.Function;

public record Page<T>(
        int currentPage,
        int perPage,
        long total,
        List<T> items
) {

    public <R> Page<R> map(final Function<T, R> mapper) {
        List<R> aNewList = this.items().stream()
                .map(mapper)
                .toList();
        return new Page<>(currentPage(), perPage(), total(), aNewList);
    }

}
