package com.hims.patient.application.exceptions;

public class InvalidPatientRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidPatientRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
