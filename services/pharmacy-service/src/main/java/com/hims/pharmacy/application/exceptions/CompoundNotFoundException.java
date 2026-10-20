package com.hims.pharmacy.application.exceptions;

public class CompoundNotFoundException extends SaleRuleException {
    public CompoundNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
