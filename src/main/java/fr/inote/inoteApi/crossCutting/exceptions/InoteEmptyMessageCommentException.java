package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteEmptyMessageCommentException extends Exception {
    public InoteEmptyMessageCommentException() {
        super(MessagesEn.COMMENT_ERROR_MESSAGE_IS_EMPTY);
    }
}
