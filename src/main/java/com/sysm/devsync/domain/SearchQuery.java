package com.sysm.devsync.domain;

public record SearchQuery(
        Page page,
        String terms
) {

    public static SearchQuery of(Page page) {
        return new SearchQuery(page, null);
    }

    public static SearchQuery of(Page page, String terms) {
        return new SearchQuery(page, terms);
    }

}
