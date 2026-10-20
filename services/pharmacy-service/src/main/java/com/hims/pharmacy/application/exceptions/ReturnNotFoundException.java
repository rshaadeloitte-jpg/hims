package com.hims.pharmacy.application.exceptions;

/** Return lookup 404 — mirrors SaleNotFoundException pattern. */
public class ReturnNotFoundException extends RuntimeException {
    public ReturnNotFoundException(String code, String message) {
        super(message);
    }
}
