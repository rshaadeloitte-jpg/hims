package com.hims.reporting.application.exceptions;

/** Unknown report code or execution id (maps to 404). */
public class ReportingNotFoundException extends ReportingRuleException {

    public ReportingNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
