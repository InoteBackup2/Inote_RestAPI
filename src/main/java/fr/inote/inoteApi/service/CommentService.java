package fr.inote.inoteApi.service;

import java.util.List;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.dto.CommentDtoResponse;
import fr.inote.inoteApi.entity.Comment;

public interface CommentService {

    Comment createComment(String message) throws InoteEmptyMessageCommentException;

    public List<CommentDtoResponse> getAll();

}
