package com.hims.adt.application.exceptions;

/** Patient already has an active (non-cancelled, not discharged) admission — BR-IPD-001 (409, BERB013). */
public class ActiveAdmissionExistsException extends AdtRuleException {

    public ActiveAdmissionExistsException(String errorCode, String message) {
        super(errorCode, message);
    }
}
