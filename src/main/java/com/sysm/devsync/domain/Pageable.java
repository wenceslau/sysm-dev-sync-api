package com.sysm.devsync.domain;

public record Pageable(
        int page,
        int perPage,
        String sort,
        String direction
) {
}
