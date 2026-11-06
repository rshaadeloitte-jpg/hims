package com.hims.grievance.application.exceptions;

/** 404-class lookup miss. */
public class GrievanceNotFoundException extends RuntimeException {

    private final String errorCode;

    public GrievanceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
