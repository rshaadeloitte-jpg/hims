package com.hims.radiology.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code; lifecycle guards use
 * RAD_ILLEGAL_TRANSITION (sibling ordering-service ORD_* convention).
 */
public class InvalidRadOrderException extends RadOrderRuleException {

    public InvalidRadOrderException(String errorCode, String message) {
        super(errorCode, message);
    }
}
