package com.hims.search.application.exceptions;

/**
 * Invalid request payload or illegal lifecycle transition (maps to 400).
 * {@code BEXA003} is the legacy generic validation code; routing/retry guards
 * use SEARCH_* codes.
 */
public class InvalidSearchRequestException extends SearchRuleException {

    public InvalidSearchRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
