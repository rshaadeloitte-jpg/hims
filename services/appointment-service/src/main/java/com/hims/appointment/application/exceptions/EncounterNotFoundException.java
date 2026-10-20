package com.hims.appointment.application.exceptions;

/** 404 envelope carrier for unknown encounters. */
public class EncounterNotFoundException extends RuntimeException {
    private final String errorCode;

    public EncounterNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
