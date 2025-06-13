package com.sysm.devsync.domain.enums;

public enum Role {
    ADMIN("ADMIN"),
    MEMBER("MEMBER");

    private final String value;

    Role(String value) {
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
