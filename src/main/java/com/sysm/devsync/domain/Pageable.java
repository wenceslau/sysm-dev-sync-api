package com.sysm.devsync.domain;

public record Pageable(
        int page,
        int perPage,
        String sort,
        String direction
) {
    public static Pageable of(int page, int perPage){
        return new Pageable(page, perPage, "id", "asc");
    }

    public static Pageable of(int page, int perPage, String sort, String direction){
        return new Pageable(page, perPage, sort, direction);
    }
}
