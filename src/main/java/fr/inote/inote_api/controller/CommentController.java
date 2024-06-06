package fr.inote.inote_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.inote.inote_api.cross_cutting.constants.Endpoint;
import fr.inote.inote_api.cross_cutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inote_api.cross_cutting.security.impl.JwtServiceImpl;
import fr.inote.inote_api.dto.CommentRequestDto;
import fr.inote.inote_api.dto.CommentResponseDto;
import fr.inote.inote_api.entity.Comment;
import fr.inote.inote_api.service.CommentService;
import fr.inote.inote_api.service.impl.UserServiceImpl;

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

    public CommentController(CommentService commentService, AuthenticationManager authenticationManager,
            UserServiceImpl userService, JwtServiceImpl jwtService) {
        this.commentService = commentService;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping(Endpoint.CREATE_COMMENT)
    public ResponseEntity<CommentResponseDto> create(@RequestBody CommentRequestDto commentRequestDto) throws InoteEmptyMessageCommentException{
        Comment returnValue = this.commentService.createComment(commentRequestDto.msg());
        
        CommentResponseDto returnDtoValue = 
            new CommentResponseDto(
                    returnValue.getId(), 
                    returnValue.getMessage(),
                    returnValue.getUser().getId());
        return ResponseEntity
            .status(201)
            .body(returnDtoValue);
    }

    @GetMapping(value = Endpoint.COMMENT_GET_ALL, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<CommentResponseDto>> getComments() {
        return new ResponseEntity<>(this.commentService.getAll(), HttpStatus.OK);
    }
}
