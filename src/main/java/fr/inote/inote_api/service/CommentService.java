package fr.inote.inote_api.service;

import java.util.List;

import fr.inote.inote_api.cross_cutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inote_api.dto.CommentResponseDto;
import fr.inote.inote_api.entity.Comment;

public interface CommentService {

    Comment createComment(String message) throws InoteEmptyMessageCommentException;

    public List<CommentResponseDto> getAll();

}
