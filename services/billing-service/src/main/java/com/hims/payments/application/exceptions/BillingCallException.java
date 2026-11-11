package com.hims.payments.application.exceptions;

/**
 * The synchronous settlement call to billing-service failed or returned an error.
 * Mapped to 502 BILLING_UNREACHABLE: the payment is NOT recorded and NO E-08 is
 * emitted (outbox append happens only after a successful settlement — documented
 * saga deviation in GAP-REGISTER-4 WP-13).
 */
public class BillingCallException extends RuntimeException {

    public BillingCallException(String message) {
        super(message);
    }

    public BillingCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
