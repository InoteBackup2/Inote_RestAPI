package fr.inote.inote_api.cross_cutting.exceptions;

import fr.inote.inote_api.cross_cutting.constants.MessagesEn;

public class InoteJwtNotFoundException extends Exception {
    
    public InoteJwtNotFoundException() {
        super(MessagesEn.TOKEN_ERROR_NOT_FOUND);
    }
}
