package com.hims.ordering.application.exceptions;

/** Order validation / illegal-transition rejection (400 with the rule code). */
public class InvalidOrderException extends RuntimeException {

    private final String errorCode;

    public InvalidOrderException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
