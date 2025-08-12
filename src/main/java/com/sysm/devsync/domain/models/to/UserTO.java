package com.sysm.devsync.domain.models.to;

public record UserTO(String id, String name) {

    public static UserTO of(String id, String name) {
        return new UserTO(id, name);
    }

    public static UserTO of(String id) {
        return new UserTO(id, null);
    }
}
