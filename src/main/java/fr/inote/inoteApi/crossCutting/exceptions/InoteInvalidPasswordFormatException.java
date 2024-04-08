package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_ERROR_INVALID_PASSWORD_FORMAT;

public class InoteInvalidPasswordFormatException extends Exception {
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public InoteInvalidPasswordFormatException() {
        super(EMAIL_ERROR_INVALID_PASSWORD_FORMAT);
    }
}
