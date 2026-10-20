package com.hims.adt.application.exceptions;

/** Invalid request payload or illegal lifecycle transition (maps to 400). E.g. BERA022, BERB005. */
public class InvalidAdtRequestException extends AdtRuleException {

    public InvalidAdtRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
