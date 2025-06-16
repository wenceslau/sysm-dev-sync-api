package com.sysm.devsync.domain;

public class NotFoundException extends RuntimeException {

    private final String id;

    public NotFoundException(String message, String id) {
        super(message, null, true, true);
        this.id = id;
    }

    public NotFoundException(String message, String id, Throwable cause) {
        super(message, cause);
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
