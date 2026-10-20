package com.hims.pharmacy.application.exceptions;

public class HomeMedicationNotFoundException extends SaleRuleException {
    public HomeMedicationNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
