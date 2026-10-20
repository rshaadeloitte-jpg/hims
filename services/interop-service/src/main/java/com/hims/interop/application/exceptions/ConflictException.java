package com.hims.interop.application.exceptions;

/**
 * Duplicate natural key (maps to 409). Used for ABDM correlation-id uniqueness:
 * POST /api/v1/abdm/requests with an already-known correlationId -> 409
 * INTEROP_DUPLICATE_CORRELATION_ID (register WP-T1).
 */
public class ConflictException extends InteropRuleException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
