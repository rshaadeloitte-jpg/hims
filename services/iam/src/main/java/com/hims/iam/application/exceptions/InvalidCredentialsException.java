package com.hims.iam.application.exceptions;

/** Login failure — unknown user, inactive user or bad password. */
public class InvalidCredentialsException extends RuntimeException {

    private final String errorCode;

    public InvalidCredentialsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
