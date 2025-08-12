package com.sysm.devsync.domain.enums;

public enum QueryType {
    AND, OR, IN;

    public static QueryType of(String type) {
        if (type == null)
            return OR;
        return QueryType.valueOf(type.toUpperCase());
    }
}
