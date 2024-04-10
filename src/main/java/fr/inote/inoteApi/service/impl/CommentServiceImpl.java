package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.dto.CommentDto;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {
    private CommentRepository commentRepository;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment createComment(CommentDto commentDto) throws InoteEmptyMessageCommentException {
        // Get current user
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (commentDto.message().isBlank() || commentDto.message().isEmpty()) {
            throw new InoteEmptyMessageCommentException();
        }
        Comment commentToWrite = Comment.builder()
                .user(user)
                .message(commentDto.message())
                .build();
        return this.commentRepository.save(commentToWrite);
    }
}
