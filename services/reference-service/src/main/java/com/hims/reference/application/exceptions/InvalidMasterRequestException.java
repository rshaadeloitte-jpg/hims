package com.hims.reference.application.exceptions;

/**
 * Invalid request (legacy BEXA003 = "Invalid input data !").
 */
public class InvalidMasterRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidMasterRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
