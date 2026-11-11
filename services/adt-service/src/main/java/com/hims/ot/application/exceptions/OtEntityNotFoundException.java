package com.hims.ot.application.exceptions;

/** 404 — referenced OT schedule/detail does not exist. */
public class OtEntityNotFoundException extends RuntimeException {

    private final String errorCode;

    public OtEntityNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
