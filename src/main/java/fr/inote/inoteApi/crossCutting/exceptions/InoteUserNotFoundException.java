package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.USER_ERROR_USER_NOT_FOUND;

public class InoteUserNotFoundException extends Exception {
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public InoteUserNotFoundException() {
        super(USER_ERROR_USER_NOT_FOUND);
    }
}
