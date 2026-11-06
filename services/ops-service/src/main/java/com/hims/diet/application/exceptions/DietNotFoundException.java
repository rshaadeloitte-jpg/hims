package com.hims.diet.application.exceptions;

/** 404-class lookup miss. */
public class DietNotFoundException extends RuntimeException {

    private final String errorCode;

    public DietNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
