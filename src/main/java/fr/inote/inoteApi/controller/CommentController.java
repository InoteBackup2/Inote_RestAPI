package fr.inote.inoteApi.controller;

import fr.inote.inoteApi.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
    private final CommentService commentService;

    @Autowired

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
}
