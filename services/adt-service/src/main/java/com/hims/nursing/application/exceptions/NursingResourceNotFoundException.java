package com.hims.nursing.application.exceptions;

/** Nursing resource (task/note/vital) not found (maps to 404). */
public class NursingResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public NursingResourceNotFoundException(String errorCode, String message) {
        super(errorCode + ": " + message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
