package com.sysm.devsync.domain;

import com.sysm.devsync.domain.enums.QueryType;

import java.util.Map;

public record SearchQuery(
        Page page,
        QueryType queryType,
        Map<String, String> terms) {

    public static SearchQuery of(Page page) {
        return new SearchQuery(page, QueryType.AND, null);
    }

    public static SearchQuery of(Page page, Map<String, String> terms) {
        return new SearchQuery(page, QueryType.AND, terms);
    }

    public static SearchQuery of(Page page, QueryType queryType, Map<String, String> terms) {
        return new SearchQuery(page, queryType, terms);
    }

}
