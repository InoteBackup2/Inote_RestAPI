package fr.inote.inote_api.cross_cutting.exceptions;

import static fr.inote.inote_api.cross_cutting.constants.MessagesEn.EMAIL_ERROR_INVALID_EMAIL_FORMAT;

public class InoteInvalidEmailFormat extends Exception{
    public InoteInvalidEmailFormat(){
        super(EMAIL_ERROR_INVALID_EMAIL_FORMAT);
    }
}
