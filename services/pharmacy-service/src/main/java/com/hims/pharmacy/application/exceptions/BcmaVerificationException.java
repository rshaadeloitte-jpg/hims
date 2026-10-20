package com.hims.pharmacy.application.exceptions;

public class BcmaVerificationException extends SaleRuleException {
    public BcmaVerificationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
