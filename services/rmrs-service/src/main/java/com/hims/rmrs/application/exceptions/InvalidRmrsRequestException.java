package com.hims.rmrs.application.exceptions;

/**
 * Invalid request payload or violated validation guard (maps to 400).
 * {@code BEXA003} is the legacy generic validation code (register WP-R5:
 * "validates head/ledger existence + head-ledger consistency (400 otherwise)").
 */
public class InvalidRmrsRequestException extends RmrsRuleException {

    public InvalidRmrsRequestException(String message) {
        super("BEXA003", message);
    }
}
