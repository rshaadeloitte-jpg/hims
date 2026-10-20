package com.hims.pharmacy.application.exceptions;

/**
 * Illegal lifecycle transition / business-rule guard violation (maps to 400).
 * {@code PH_ILLEGAL_TRANSITION} mirrors the reference service's
 * {@code ORD_ILLEGAL_TRANSITION} convention.
 */
public class IllegalSaleStateException extends SaleRuleException {

    public IllegalSaleStateException(String errorCode, String message) {
        super(errorCode, message);
    }
}
