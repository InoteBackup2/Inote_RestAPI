package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {
    private CommentRepository commentRepository;
    private UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }




    public Comment createComment(String msg) throws InoteEmptyMessageCommentException {
        // Get current user
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if ( msg.isBlank()|| msg.isEmpty()) {
            throw new InoteEmptyMessageCommentException();
        }
        User user2 = userRepository.findByEmail(user.getEmail()).orElseThrow();
        Comment commentToWrite = Comment.builder()
                .user(user2)
                .message(msg)
                .build();
        Comment returnedComment = this.commentRepository.save(commentToWrite);
        return returnedComment;
    }
}
