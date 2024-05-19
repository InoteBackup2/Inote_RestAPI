package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.REGISTER_ERROR_USER_ALREADY_EXISTS;

public class InoteExistingEmailException extends Exception{
    public InoteExistingEmailException() {
        super(REGISTER_ERROR_USER_ALREADY_EXISTS);
    }

}
