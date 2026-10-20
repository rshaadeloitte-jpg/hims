package com.hims.patient.application.exceptions;

public class PatientNotFoundException extends RuntimeException {

    private final String errorCode;

    public PatientNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
