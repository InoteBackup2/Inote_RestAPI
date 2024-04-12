package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * The Service CommentServiceImpl
 * @author Atsuhiko Mochizuki
 * @date   11/04/2024
 */
@Service
public class CommentServiceImpl implements CommentService {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    private CommentRepository commentRepository;
    private UserRepository userRepository;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    /* PUBLIC METHODS */
    /* ============================================================ */

    /**
     * create a comment
     * 
     * @param msg
     * @return Comment
     * @throws InoteEmptyMessageCommentException
     * 
     * @author Atsuhiko Mochizuki
     * @date 11/04/2024
     */
    public Comment createComment(String msg) throws InoteEmptyMessageCommentException {
        // Get current user
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (msg.isBlank() || msg.isEmpty()) {
            throw new InoteEmptyMessageCommentException();
        }
        User foundedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        Comment commentToWrite = Comment.builder()
                .user(foundedUser)
                .message(msg)
                .build();
        return this.commentRepository.save(commentToWrite);
    }
}
