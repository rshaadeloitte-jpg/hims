package com.hims.reference.application.exceptions;

/**
 * Entity not found. Legacy crudget returns an empty body for unknown ids; the new
 * contract returns 404 with the legacy error code semantics (documented deviation —
 * gateway maps it; DECISIONS IMP-DEC-011).
 */
public class MasterNotFoundException extends RuntimeException {

    private final String errorCode;

    public MasterNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
