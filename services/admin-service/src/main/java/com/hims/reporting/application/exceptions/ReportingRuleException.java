package com.hims.reporting.application.exceptions;

/**
 * Base rule violation carrying the legacy-style error code (contract per 04 §9.6).
 * Codes used by Reporting: BEXA003 (generic validation),
 * REPORTING_UNKNOWN_EVENT_TYPE (ingress routing),
 * REPORTING_DUPLICATE_REPORT_CODE (409 per register WP-T2),
 * REPORTING_INVALID_DEFINITION (report engine whitelist), NOT_FOUND.
 */
public class ReportingRuleException extends RuntimeException {

    private final String errorCode;

    public ReportingRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
