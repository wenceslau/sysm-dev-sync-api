package com.sysm.devsync.domain;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message, null, true, true);
    }

    public BusinessException(String message, String id, Throwable cause) {
        super(message, cause);
    }

}
