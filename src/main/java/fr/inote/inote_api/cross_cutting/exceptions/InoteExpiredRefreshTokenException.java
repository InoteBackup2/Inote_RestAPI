package fr.inote.inote_api.cross_cutting.exceptions;

import fr.inote.inote_api.cross_cutting.constants.MessagesEn;

public class InoteExpiredRefreshTokenException extends Exception {
    public InoteExpiredRefreshTokenException() {
        super(MessagesEn.TOKEN_ERROR_REFRESH_TOKEN_EXPIRED);
    }
}
