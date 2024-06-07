package fr.inote.inote_api.controller;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final MessageSource source;

    public HelloController(MessageSource source){
        this.source = source;
    }

    @GetMapping(path = "/api/hello")
    public ResponseEntity<String> hello(
            @RequestHeader(name = "Accept-Language", required = false)
            final Locale locale,
            @RequestParam(name = "user", required = false, defaultValue = "")
            final String USER){
        return ResponseEntity
            .status(HttpStatus.OK)
            .header("Content-Type", "text/plain")
            .body(source.getMessage("hello", new Object[]{USER}, locale));
    }
}
