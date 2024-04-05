package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.VALIDATION_ERROR_VALIDATION_IS_EXPIRED;

public class InoteValidationExpiredException extends Exception {
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public InoteValidationExpiredException() {
        super(VALIDATION_ERROR_VALIDATION_IS_EXPIRED);
    }

}
