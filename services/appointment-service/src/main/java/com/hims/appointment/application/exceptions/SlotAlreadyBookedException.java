package com.hims.appointment.application.exceptions;

/** Slot conflict (maps to 409) — BR-OPD-002 slot rules / unique-slot constraint. */
public class SlotAlreadyBookedException extends RuntimeException {

    private final String errorCode;

    public SlotAlreadyBookedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
