package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteEmptyMessageCommentException extends Exception {
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public InoteEmptyMessageCommentException() {
        super(MessagesEn.COMMENT_ERROR_MESSAGE_IS_EMPTY);
    }
}
