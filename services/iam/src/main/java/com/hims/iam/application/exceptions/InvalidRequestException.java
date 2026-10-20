package com.hims.iam.application.exceptions;

/** Semantic rule violation on an otherwise well-formed request. */
public class InvalidRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
