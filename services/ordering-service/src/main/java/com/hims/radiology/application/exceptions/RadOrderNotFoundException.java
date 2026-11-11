package com.hims.radiology.application.exceptions;

/** Unknown rad order id (maps to 404). */
public class RadOrderNotFoundException extends RadOrderRuleException {

    public RadOrderNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
