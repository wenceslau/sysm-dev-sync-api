package com.sysm.devsync.domain;

public record SearchQuery(
        Pageable pageable,
        String terms
) {

    public static SearchQuery of(Pageable pageable) {
        return new SearchQuery(pageable, null);
    }

    public static SearchQuery of(Pageable pageable, String terms) {
        return new SearchQuery(pageable, terms);
    }

}
