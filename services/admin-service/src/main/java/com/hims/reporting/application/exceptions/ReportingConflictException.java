package com.hims.reporting.application.exceptions;

/**
 * Duplicate report_code on POST /api/v1/reports (maps to 409 envelope per
 * register WP-T2 "409 dup code"; same pattern as rcm-service TidConflictException).
 */
public class ReportingConflictException extends ReportingRuleException {

    public ReportingConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
