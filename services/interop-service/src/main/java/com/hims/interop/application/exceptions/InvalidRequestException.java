package com.hims.interop.application.exceptions;

/** Invalid request payload or illegal state transition (maps to 400). */
public class InvalidRequestException extends InteropRuleException {

    public InvalidRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
