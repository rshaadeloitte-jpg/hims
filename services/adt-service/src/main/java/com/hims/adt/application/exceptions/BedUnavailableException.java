package com.hims.adt.application.exceptions;

/** Bed allocation failure: no free bed or lost CAS claim (BR-IPD-002; maps to 409, code BEXB001). */
public class BedUnavailableException extends AdtRuleException {

    public BedUnavailableException(String errorCode, String message) {
        super(errorCode, message);
    }
}
