package com.hims.govt.application.exceptions;

/** Unknown resource id / enrolment number (maps to 404). */
public class GovtNotFoundException extends GovtRuleException {

    public GovtNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
