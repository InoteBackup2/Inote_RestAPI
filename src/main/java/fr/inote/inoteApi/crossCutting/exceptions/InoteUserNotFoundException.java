package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.USER_ERROR_USER_NOT_FOUND;

public class InoteUserNotFoundException extends Exception {
    public InoteUserNotFoundException() {
        super(USER_ERROR_USER_NOT_FOUND);
    }
}
