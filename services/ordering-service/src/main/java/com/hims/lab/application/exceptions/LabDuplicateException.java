package com.hims.lab.application.exceptions;

/** Duplicate entity (maps to 409). */
public class LabDuplicateException extends LabRuleException {

    public LabDuplicateException(String errorCode, String message) {
        super(errorCode, message);
    }
}
