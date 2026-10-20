package com.hims.reporting.application.exceptions;

/**
 * Invalid request payload / illegal ingress event / invalid report definition
 * (maps to 400). {@code BEXA003} is the legacy generic validation code; the
 * ingress unknown-eventType guard uses REPORTING_UNKNOWN_EVENT_TYPE.
 */
public class InvalidReportingRequestException extends ReportingRuleException {

    public InvalidReportingRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
