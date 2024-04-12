package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.VALIDATION_ERROR_VALIDATION_IS_EXPIRED;

public class InoteValidationExpiredException extends Exception {
   
    public InoteValidationExpiredException() {
        super(VALIDATION_ERROR_VALIDATION_IS_EXPIRED);
    }

}
