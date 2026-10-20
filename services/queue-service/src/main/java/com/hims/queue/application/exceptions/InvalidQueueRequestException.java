package com.hims.queue.application.exceptions;

public class InvalidQueueRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidQueueRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
