package com.hims.adt.application.exceptions;

/** Admission/ward/bed not found (maps to 404). */
public class AdtEntityNotFoundException extends AdtRuleException {

    public AdtEntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
