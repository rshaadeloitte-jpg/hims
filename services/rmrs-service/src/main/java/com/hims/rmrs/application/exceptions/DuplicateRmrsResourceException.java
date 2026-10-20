package com.hims.rmrs.application.exceptions;

/**
 * Unique-key violation caught before insert (maps to 409, sibling tid-uniqueness
 * convention): duplicate receipt_no on a collection, or duplicate master code.
 */
public class DuplicateRmrsResourceException extends RmrsRuleException {

    public DuplicateRmrsResourceException(String errorCode, String message) {
        super(errorCode, message);
    }
}
