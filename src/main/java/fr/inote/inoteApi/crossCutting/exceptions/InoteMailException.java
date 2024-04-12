package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteMailException extends Exception{
    public InoteMailException(){
        super(MessagesEn.EMAIL_ERROR_POSSIBLE_SMTP_SERVEUR_NOT_CONFIGURED);
    }
}
