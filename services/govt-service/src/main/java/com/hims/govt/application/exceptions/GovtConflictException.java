package com.hims.govt.application.exceptions;

/** Uniqueness conflict — duplicate enrolmentNo / tid / ashaCode (maps to 409). */
public class GovtConflictException extends GovtRuleException {

    public GovtConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
