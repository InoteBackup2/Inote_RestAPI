package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteNotAuthenticatedUserException extends Exception {
    public InoteNotAuthenticatedUserException() {
        super(MessagesEn.USER_NOT_AUTHENTICATED);
    }
}
