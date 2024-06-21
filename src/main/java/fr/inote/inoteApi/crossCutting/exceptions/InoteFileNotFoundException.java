package fr.inote.inoteApi.crossCutting.exceptions;

import fr.inote.inoteApi.crossCutting.constants.MessagesEn;

public class InoteFileNotFoundException extends Exception{
 public InoteFileNotFoundException() {
        super(MessagesEn.FILE_NOT_FOUND_IN_INOTE_STORAGE_FOLDER);
    }
}
