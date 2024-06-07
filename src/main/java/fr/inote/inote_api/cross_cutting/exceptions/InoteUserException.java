package fr.inote.inote_api.cross_cutting.exceptions;
import fr.inote.inote_api.cross_cutting.constants.MessagesEn;

public class InoteUserException extends Exception {
    public InoteUserException() {
        super(MessagesEn.UNSPECIFIED_ERROR_HAS_OCCURED);
    }
}
