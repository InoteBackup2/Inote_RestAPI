package fr.inote.inoteApi.service;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.entity.Comment;

public interface CommentService {

    Comment createComment(String message) throws InoteEmptyMessageCommentException;

}
