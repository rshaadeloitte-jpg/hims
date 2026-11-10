package com.hims.payments.application.exceptions;

/** Unknown entity -> 404 NOT_FOUND (same shape as sibling services). */
public class PaymentEntityNotFoundException extends RuntimeException {

    public PaymentEntityNotFoundException(String message) {
        super(message);
    }
}
