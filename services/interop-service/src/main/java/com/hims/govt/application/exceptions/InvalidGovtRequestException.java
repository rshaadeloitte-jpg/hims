package com.hims.govt.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code; guards use
 * GOVT_* rule codes (GOVT_ILLEGAL_TRANSITION, GOVT_CASE_CLOSED).
 */
public class InvalidGovtRequestException extends GovtRuleException {

    public InvalidGovtRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
