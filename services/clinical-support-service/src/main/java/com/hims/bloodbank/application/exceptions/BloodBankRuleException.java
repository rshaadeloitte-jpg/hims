package com.hims.bloodbank.application.exceptions;

/*
 * Base for blood-bank rule violations carrying an error code
 * (codes BLD_REQ_INVALID / BLD_STATE / BLD_BAG_CONFLICT / BLD_NOT_FOUND).
 */
public abstract class BloodBankRuleException extends RuntimeException {

    private final String errorCode;

    protected BloodBankRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
