package fr.inote.inoteApi.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.inote.inoteApi.dto.PublicUserResponseDto;
import fr.inote.inoteApi.dto.UserRequestDto;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

import static fr.inote.inoteApi.crossCutting.constants.Endpoint.GET_ALL_USERS;
import static fr.inote.inoteApi.crossCutting.constants.Endpoint.USER;

import java.util.List;

/**
 * Controller for related Users operations
 * 
 * @author atsuhiko Mochizuki
 * @date 2024-06-11
 */
@RestController
public class UserController {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

    /* Endpoints */
    /* ============================================================ */
    @PostMapping(path = USER)
    public ResponseEntity<PublicUserResponseDto> getUser(@RequestBody UserRequestDto userRequestDto) {
        User requestedUser = this.userService.loadUserByUsername(userRequestDto.username());

        PublicUserResponseDto publicUserResponseDto = new PublicUserResponseDto(
                requestedUser.getUsername(),
                requestedUser.getPseudonyme(),
                requestedUser.getAvatar(),
                requestedUser.isActif(), requestedUser.getRole());

        return ResponseEntity
                .status(HttpStatusCode.valueOf(200))
                .body(publicUserResponseDto);
    }

    @GetMapping(path = GET_ALL_USERS)
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = this.userService.list();
        return ResponseEntity
                .status(HttpStatusCode.valueOf(200))
                .body(users);
    }
   
}
