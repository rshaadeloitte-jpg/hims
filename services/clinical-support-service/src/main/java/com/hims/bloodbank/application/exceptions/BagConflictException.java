package com.hims.bloodbank.application.exceptions;

/**
 * Bag already crossmatched/issued/expired — maps to 409 per GAP-REGISTER WP-9
 * (crossmatch/issue guards).
 */
public class BagConflictException extends BloodBankRuleException {

    public BagConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
