package com.hims.transport.application.exceptions;

/** 404-class lookup miss. */
public class TransportNotFoundException extends RuntimeException {

    private final String errorCode;

    public TransportNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
