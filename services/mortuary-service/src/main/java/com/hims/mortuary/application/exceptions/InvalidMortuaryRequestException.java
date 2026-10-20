package com.hims.mortuary.application.exceptions;

/** Invalid request payload or illegal lifecycle transition (maps to 400). */
public class InvalidMortuaryRequestException extends MortuaryRuleException {

    public InvalidMortuaryRequestException(String errorCode, String message) {
        super(errorCode, message);
    }
}
