package com.hims.waste.application.exceptions;

/** 400-class request violation (BEXA003 invalid input unless a specific code is given). */
public class InvalidWasteRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidWasteRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
