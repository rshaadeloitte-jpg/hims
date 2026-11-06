package com.hims.bloodbank.application.exceptions;

/** Donor/requisition/bag not found (maps to 404). */
public class BloodBankEntityNotFoundException extends BloodBankRuleException {

    public BloodBankEntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
