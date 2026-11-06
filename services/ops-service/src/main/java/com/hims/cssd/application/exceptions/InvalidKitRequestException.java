package com.hims.cssd.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code (register WP-R3:
 * "BEXA003-style 400"); lifecycle guards use CSSD_ILLEGAL_TRANSITION.
 */
public class InvalidKitRequestException extends CssdRuleException {

    public InvalidKitRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
