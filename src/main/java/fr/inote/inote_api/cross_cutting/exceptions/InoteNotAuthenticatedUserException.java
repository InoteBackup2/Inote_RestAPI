package fr.inote.inote_api.cross_cutting.exceptions;

import fr.inote.inote_api.cross_cutting.constants.MessagesEn;

public class InoteNotAuthenticatedUserException extends Exception {
    public InoteNotAuthenticatedUserException() {
        super(MessagesEn.USER_NOT_AUTHENTICATED);
    }
}
