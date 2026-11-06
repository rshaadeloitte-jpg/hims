package com.hims.linen.application.exceptions;

/** 404-class lookup miss. */
public class LinenNotFoundException extends RuntimeException {

    private final String errorCode;

    public LinenNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
