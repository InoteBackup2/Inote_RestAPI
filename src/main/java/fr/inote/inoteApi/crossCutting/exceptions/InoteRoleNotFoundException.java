package fr.inote.inoteApi.crossCutting.exceptions;

import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.ROLE_ERROR_NOT_FOUND;

public class InoteRoleNotFoundException extends Exception {
    public InoteRoleNotFoundException() {
        super(ROLE_ERROR_NOT_FOUND);
    }
}
