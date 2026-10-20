package com.hims.ordering.application.exceptions;

/** Unknown prescription id (maps to 404). */
public class PrescriptionNotFoundException extends OrderRuleException {

    public PrescriptionNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
