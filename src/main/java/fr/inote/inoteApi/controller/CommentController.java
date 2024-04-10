package fr.inote.inoteApi.controller;

import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.CommentDto;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.service.CommentService;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;

/**
 * Comment controller
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */
@RestController
public class CommentController {
    private final CommentService commentService;
    private final AuthenticationManager authenticationManager;
    private final UserServiceImpl userService;
    private final JwtServiceImpl jwtService;

    @Autowired
    public CommentController(CommentService commentService, AuthenticationManager authenticationManager, UserServiceImpl userService, JwtServiceImpl jwtService) {
        this.commentService = commentService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping(Endpoint.CREATE_COMMENT)
    public ResponseEntity<Comment> create(@RequestBody CommentDto commentDto) throws InoteEmptyMessageCommentException {
        Comment returnValue;
        try{
            returnValue = this.commentService.createComment(commentDto.msg());
        }catch(InoteEmptyMessageCommentException ex){
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        }
        return new ResponseEntity<>(returnValue, HttpStatus.CREATED);
    }
}
