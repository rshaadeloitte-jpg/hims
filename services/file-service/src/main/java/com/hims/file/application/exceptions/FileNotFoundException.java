package com.hims.file.application.exceptions;

public class FileNotFoundException extends RuntimeException {
    private final String errorCode;
    public FileNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
