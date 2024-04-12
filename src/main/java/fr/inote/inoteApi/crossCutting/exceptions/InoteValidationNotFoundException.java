package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.VALIDATION_ERROR_NOT_FOUND;

public class InoteValidationNotFoundException extends Exception{
   
    public InoteValidationNotFoundException() {
        super(VALIDATION_ERROR_NOT_FOUND);
    }
}
