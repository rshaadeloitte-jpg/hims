package com.hims.pharmacy.application.exceptions;

/**
 * Invalid request payload (maps to 400). {@code BEXA003} is the legacy generic
 * validation code, same as the ordering-service reference.
 */
public class InvalidSaleRequestException extends SaleRuleException {

    public InvalidSaleRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
