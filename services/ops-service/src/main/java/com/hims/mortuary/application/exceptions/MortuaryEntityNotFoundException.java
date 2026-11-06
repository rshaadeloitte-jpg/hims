package com.hims.mortuary.application.exceptions;

/** Dead body / autopsy / release record not found (maps to 404). */
public class MortuaryEntityNotFoundException extends MortuaryRuleException {

    public MortuaryEntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
