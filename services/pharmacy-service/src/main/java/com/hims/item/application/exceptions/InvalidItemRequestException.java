package com.hims.item.application.exceptions;

/** Invalid request (legacy BEXA003-style code carried in the envelope). */
public class InvalidItemRequestException extends RuntimeException {

    private final String errorCode;

    public InvalidItemRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
