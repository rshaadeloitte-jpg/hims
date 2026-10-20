package com.hims.rcm.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code; the NHCX status-machine
 * guard uses RCM_ILLEGAL_TRANSITION.
 */
public class InvalidRcmRequestException extends RcmRuleException {

    public InvalidRcmRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
