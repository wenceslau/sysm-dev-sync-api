package com.sysm.devsync.domain;

public record Page(
        int pageNumber,
        int pageSize,
        String sort,
        String direction
) {
    public static Page of(int pageNumber, int pageSize){
        return new Page(pageNumber, pageSize, "id", "asc");
    }

    public static Page of(int page, int pageSize, String sort, String direction){
        return new Page(page, pageSize, sort, direction);
    }
}
