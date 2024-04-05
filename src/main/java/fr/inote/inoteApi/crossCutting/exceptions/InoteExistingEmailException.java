package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.REGISTER_ERROR_USER_ALREADY_EXISTS;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.NOT_ACCEPTABLE, reason="Record not found") //
public class InoteExistingEmailException extends Exception{
    public InoteExistingEmailException() {
        super(REGISTER_ERROR_USER_ALREADY_EXISTS);
    }

}
