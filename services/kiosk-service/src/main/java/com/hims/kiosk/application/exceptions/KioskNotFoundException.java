package com.hims.kiosk.application.exceptions;

public class KioskNotFoundException extends RuntimeException {
    private final String errorCode;
    public KioskNotFoundException(String errorCode, String message) {
        super(message); this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
