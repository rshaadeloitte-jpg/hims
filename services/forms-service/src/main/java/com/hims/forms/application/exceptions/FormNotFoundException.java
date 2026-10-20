package com.hims.forms.application.exceptions;

public class FormNotFoundException extends RuntimeException {
    private final String errorCode;
    public FormNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
