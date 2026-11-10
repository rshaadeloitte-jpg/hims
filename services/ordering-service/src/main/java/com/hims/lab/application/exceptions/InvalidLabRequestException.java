package com.hims.lab.application.exceptions;

/** Invalid request payload or illegal lifecycle transition (maps to 400). */
public class InvalidLabRequestException extends LabRuleException {

    public InvalidLabRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
