package com.hims.rcm.application.exceptions;

/** Duplicate admission TID assignment — admission already carries a TID (maps to 409 TID_EXISTS). */
public class TidConflictException extends RcmRuleException {

    public TidConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
