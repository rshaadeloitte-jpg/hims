package com.hims.kiosk.application.exceptions;

public class InvalidKioskRequestException extends RuntimeException {
    private final String errorCode;
    public InvalidKioskRequestException(String errorCode, String message) {
        super(message); this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
