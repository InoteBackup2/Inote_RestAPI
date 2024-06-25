package fr.inote.inote_api.cross_cutting.exceptions;

import static fr.inote.inote_api.cross_cutting.constants.MessagesEn.ROLE_ERROR_NOT_FOUND;

public class InoteRoleNotFoundException extends Exception {
    public InoteRoleNotFoundException() {
        super(ROLE_ERROR_NOT_FOUND);
    }
}
