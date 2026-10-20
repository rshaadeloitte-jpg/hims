package com.hims.search.application.exceptions;

/** Unknown index-request id (maps to 404). */
public class SearchNotFoundException extends SearchRuleException {

    public SearchNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
