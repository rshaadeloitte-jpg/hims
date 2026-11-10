package com.hims.nursing.application.exceptions;

/** Invalid request payload or illegal task lifecycle transition (maps to 400). */
public class InvalidNursingRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidNursingRequestException(String errorCode, String message) {
        super(errorCode + ": " + message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
