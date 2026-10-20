package com.hims.lab.application.exceptions;

/** Booking/detail/machine not found (maps to 404). */
public class LabEntityNotFoundException extends LabRuleException {

    public LabEntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
