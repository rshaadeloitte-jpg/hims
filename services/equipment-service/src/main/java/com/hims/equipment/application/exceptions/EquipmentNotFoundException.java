package com.hims.equipment.application.exceptions;

/** 404-class lookup miss. */
public class EquipmentNotFoundException extends RuntimeException {

    private final String errorCode;

    public EquipmentNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
