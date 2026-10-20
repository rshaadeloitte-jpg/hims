package com.hims.appointment.application.exceptions;

/** Invalid request payload or illegal lifecycle transition (maps to 400). */
public class InvalidAppointmentRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidAppointmentRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
