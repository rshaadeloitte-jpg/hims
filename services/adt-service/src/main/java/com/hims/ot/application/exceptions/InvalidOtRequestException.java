package com.hims.ot.application.exceptions;

/**
 * 400 — invalid request or illegal lifecycle transition. Carries the legacy
 * error code (BR-OT-001 prerequisite failure -> BEOT004).
 */
public class InvalidOtRequestException extends OtRuleException {

    public InvalidOtRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
