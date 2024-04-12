package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_ERROR_INVALID_PASSWORD_FORMAT;

public class InoteInvalidPasswordFormatException extends Exception {
    public InoteInvalidPasswordFormatException() {
        super(EMAIL_ERROR_INVALID_PASSWORD_FORMAT);
    }
}
