package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.USER_ERROR_DETECTED;

public class InoteUserException extends Exception {
    public InoteUserException() {
        super(USER_ERROR_DETECTED);
    }
}
