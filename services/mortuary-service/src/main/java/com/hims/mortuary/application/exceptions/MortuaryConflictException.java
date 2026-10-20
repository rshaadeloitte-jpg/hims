package com.hims.mortuary.application.exceptions;

/** Body already released/autopsy-conflicting state (maps to 409). */
public class MortuaryConflictException extends MortuaryRuleException {

    public MortuaryConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
