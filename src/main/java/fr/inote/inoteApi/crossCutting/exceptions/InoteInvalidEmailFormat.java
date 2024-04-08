package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_ERROR_INVALID_EMAIL_FORMAT;

public class InoteInvalidEmailFormat extends Exception{
    public InoteInvalidEmailFormat(){
        super(EMAIL_ERROR_INVALID_EMAIL_FORMAT);
    }
}
