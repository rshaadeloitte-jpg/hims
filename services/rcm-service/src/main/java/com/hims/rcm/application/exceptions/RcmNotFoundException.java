package com.hims.rcm.application.exceptions;

/** Unknown resource id / correlation id (maps to 404 envelope). */
public class RcmNotFoundException extends RcmRuleException {

    public RcmNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
