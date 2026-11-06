package com.hims.rmrs.application.exceptions;

/** Unknown resource id (maps to 404). */
public class RmrsNotFoundException extends RmrsRuleException {

    public RmrsNotFoundException(String message) {
        super("RMRS_NOT_FOUND", message);
    }
}
