package com.sysm.devsync.domain;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message, null, true, true);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
