package fr.inote.inoteApi.controller;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.CommentDtoRequest;
import fr.inote.inoteApi.dto.CommentDtoResponse;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.service.CommentService;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for account user routes
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */

/*
 * Nota:
 * The @RestController annotation is a specialized version of the @Controller
 * annotation in Spring MVC.
 * It combines the functionality of the @Controller and @ResponseBody
 * annotations, which simplifies
 * the implementation of RESTful web services.
 * When a class is annotated with @RestController, the following points apply:
 * -> It acts as a controller, handling client requests.
 * -> The @ResponseBody annotation is automatically included, allowing the
 * automatic serialization
 * of the return object into the HttpResponse.
 */
@RestController
public class CommentController {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    private final CommentService commentService;

    // Needed for security context
    @SuppressWarnings("unused")
    private final AuthenticationManager authenticationManager;

    @SuppressWarnings("unused")
    private final UserServiceImpl userService;

    @SuppressWarnings("unused")
    private final JwtServiceImpl jwtService;

    @Autowired
    public CommentController(CommentService commentService, AuthenticationManager authenticationManager,
            UserServiceImpl userService, JwtServiceImpl jwtService) {
        this.commentService = commentService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping(Endpoint.CREATE_COMMENT)
    public ResponseEntity<CommentDtoResponse> create(@RequestBody CommentDtoRequest commentDtoRequest)
            throws InoteEmptyMessageCommentException {
        Comment returnValue = null;
        try {
            returnValue = this.commentService.createComment(commentDtoRequest.msg());
        } catch (InoteEmptyMessageCommentException ex) {
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);

        }
        CommentDtoResponse returnDtoValue = new CommentDtoResponse(returnValue.getId(), returnValue.getMessage(),
                returnValue.getUser().getId());
        return new ResponseEntity<>(returnDtoValue, HttpStatus.CREATED);
    }

    @GetMapping(value = Endpoint.COMMENT_GET_ALL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<CommentDtoResponse>> getComments() {
        return new ResponseEntity<>(this.commentService.getAll(), HttpStatus.OK);
    }
}
