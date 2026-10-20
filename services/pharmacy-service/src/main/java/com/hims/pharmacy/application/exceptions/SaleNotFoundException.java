package com.hims.pharmacy.application.exceptions;

/** Unknown sale / prescription-projection id (maps to 404). */
public class SaleNotFoundException extends SaleRuleException {

    public SaleNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
