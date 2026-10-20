package com.hims.forms.application.exceptions;

public class InvalidFormRequestException extends RuntimeException {
    private final String errorCode;
    public InvalidFormRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
