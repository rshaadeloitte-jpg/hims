package com.hims.ordering.application.exceptions;

/** Unknown order id -> 404 NOT_FOUND. */
public class OrderNotFoundException extends RuntimeException {

    private final String errorCode;

    public OrderNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
