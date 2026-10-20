package com.hims.bloodbank.application.exceptions;

/** Invalid request payload or illegal lifecycle transition (maps to 400). */
public class InvalidBloodBankRequestException extends BloodBankRuleException {

    public InvalidBloodBankRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
