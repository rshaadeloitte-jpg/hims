package com.hims.billing.application.exceptions;

/** Entity lookup miss -> 404 with the rule code. */
public class BillingEntityNotFoundException extends RuntimeException {

    private final String errorCode;

    public BillingEntityNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
