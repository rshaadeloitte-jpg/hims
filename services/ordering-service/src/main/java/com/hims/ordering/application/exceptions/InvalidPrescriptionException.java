package com.hims.ordering.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code (register WP-10:
 * "BEXA003-style 400"); the cancel guard uses ORD_ILLEGAL_TRANSITION.
 */
public class InvalidPrescriptionException extends OrderRuleException {

    public InvalidPrescriptionException(String errorCode, String message) {
        super(errorCode, message);
    }
}
