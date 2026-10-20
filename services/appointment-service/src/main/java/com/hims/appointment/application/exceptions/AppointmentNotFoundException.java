package com.hims.appointment.application.exceptions;

/** Appointment or schedule not found (maps to 404). */
public class AppointmentNotFoundException extends RuntimeException {

    private final String errorCode;

    public AppointmentNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
