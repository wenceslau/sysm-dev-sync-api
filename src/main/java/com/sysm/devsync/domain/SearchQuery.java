package com.sysm.devsync.domain;

import java.util.Map;

public record SearchQuery(
        Page page,
        Map<String, String> terms) {

    public static SearchQuery of(Page page) {
        return new SearchQuery(page, null);
    }

    public static SearchQuery of(Page page,  Map<String, String> terms) {
        return new SearchQuery(page, terms);
    }

}
