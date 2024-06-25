package fr.inote.inote_api.cross_cutting.exceptions;

import fr.inote.inote_api.cross_cutting.constants.MessagesEn;

public class InoteMailException extends Exception{
    public InoteMailException(){
        super(MessagesEn.EMAIL_ERROR_POSSIBLE_SMTP_SERVEUR_NOT_CONFIGURED);
    }
}
