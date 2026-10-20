package com.hims.interop.application.exceptions;

/** Unknown message id or ABDM correlationId (maps to 404). */
public class InteropNotFoundException extends InteropRuleException {

    public InteropNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
