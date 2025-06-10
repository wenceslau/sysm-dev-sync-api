package com.sysm.devsync.domain;

public enum ROLE {
    ADMIN("ADMIN"),
    MEMBER("MEMBER");

    private final String value;

    ROLE(String value) {
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
