package com.hims.item.application.exceptions;

/** Record not found -> HTTP 404 with the legacy-style code in the envelope. */
public class ItemNotFoundException extends RuntimeException {

    private final String errorCode;

    public ItemNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
