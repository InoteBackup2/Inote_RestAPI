package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteJwtNotFoundException extends Exception {
    
    public InoteJwtNotFoundException() {
        super(MessagesEn.TOKEN_ERROR_NOT_FOUND);
    }
}
