package com.hims.search.application.exceptions;

/**
 * Base rule violation carrying the legacy-style error code (contract per 04 §9.6).
 * Codes used by search: BEXA003 (generic validation), SEARCH_UNKNOWN_EVENT_TYPE,
 * SEARCH_UNKNOWN_COLLECTION, SEARCH_ILLEGAL_TRANSITION (retry guard),
 * NOT_FOUND (unknown index-request id).
 */
public class SearchRuleException extends RuntimeException {

    private final String errorCode;

    public SearchRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
