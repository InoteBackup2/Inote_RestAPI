package fr.inote.inoteApi.service;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.dto.CommentDto;
import fr.inote.inoteApi.entity.Comment;

public interface CommentService {

    Comment createComment(CommentDto commentDto) throws InoteEmptyMessageCommentException;

}
