package com.hims.queue.application.exceptions;

public class QueueNotFoundException extends RuntimeException {

    private final String errorCode;

    public QueueNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
