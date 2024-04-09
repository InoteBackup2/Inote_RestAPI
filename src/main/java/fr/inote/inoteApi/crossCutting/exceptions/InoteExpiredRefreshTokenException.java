package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteExpiredRefreshTokenException extends Exception {
    public InoteExpiredRefreshTokenException() {
        super(MessagesEn.TOKEN_ERROR_REFRESH_TOKEN_EXPIRED);
    }
}
