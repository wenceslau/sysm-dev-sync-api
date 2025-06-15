package com.sysm.devsync.domain;

public record SearchQuery(
        Pageable pageable,
        String terms
) {
}
