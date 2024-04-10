package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
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

    public Comment createComment(String msg) throws InoteEmptyMessageCommentException {
        // Get current user
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ( msg.isBlank()|| msg.isEmpty()) {
            throw new InoteEmptyMessageCommentException();
        }
        Comment commentToWrite = Comment.builder()
                .user(user)
                .message(msg)
                .build();
        return this.commentRepository.save(commentToWrite);
    }
}
