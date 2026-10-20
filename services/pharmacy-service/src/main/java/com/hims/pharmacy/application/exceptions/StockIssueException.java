package com.hims.pharmacy.application.exceptions;

/**
 * The synchronous item-service stock movement call failed (connection error,
 * non-2xx response or unparseable body). Thrown INSIDE the dispense transaction so
 * the whole tx rolls back: sale stays PENDING, nothing is marked dispensed and —
 * per GAP-REGISTER-5 WP-R2 — no outbox row exists anyway (DispensingCompleted is
 * PROPOSED, not in the catalog; we emit NOTHING).
 */
public class StockIssueException extends RuntimeException {

    public StockIssueException(String message) {
        super(message);
    }

    public StockIssueException(String message, Throwable cause) {
        super(message, cause);
    }
}
