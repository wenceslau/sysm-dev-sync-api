package com.sysm.devsync.domain.enums;

public enum RoleUser {
    ADMIN("ADMIN"),
    MEMBER("MEMBER");

    private final String value;

    RoleUser(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
