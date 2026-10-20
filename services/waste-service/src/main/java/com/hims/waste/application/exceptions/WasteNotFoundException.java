package com.hims.waste.application.exceptions;

/** 404-class lookup miss. */
public class WasteNotFoundException extends RuntimeException {

    private final String errorCode;

    public WasteNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
