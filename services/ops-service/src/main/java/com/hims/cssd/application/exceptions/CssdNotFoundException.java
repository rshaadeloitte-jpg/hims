package com.hims.cssd.application.exceptions;

/** Unknown kit or sterilization-cycle id (maps to 404). */
public class CssdNotFoundException extends CssdRuleException {

    public CssdNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
