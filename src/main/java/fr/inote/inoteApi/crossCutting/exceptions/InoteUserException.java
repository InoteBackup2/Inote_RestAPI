package fr.inote.inoteApi.crossCutting.exceptions;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteUserException extends Exception {
    public InoteUserException() {
        super(MessagesEn.UNSPECIFIED_ERROR_HAS_OCCURED);
    }
}
