package fr.inote.inote_api.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.inote.inote_api.dto.ProtectedUserResponseDto;
import fr.inote.inote_api.dto.PublicUserResponseDto;
import fr.inote.inote_api.dto.UserRequestDto;
import fr.inote.inote_api.entity.User;
import fr.inote.inote_api.service.impl.UserServiceImpl;

import static fr.inote.inote_api.cross_cutting.constants.Endpoint.GET_ALL_USERS;
import static fr.inote.inote_api.cross_cutting.constants.Endpoint.USER;

import java.util.ArrayList;
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

    /**
     * Get list of all registered users with useful informations for frontend 
     * 
     * @return List of ProtectedUseRequestDto or "[]" if empty
     */
    @GetMapping(path = GET_ALL_USERS)
    public ResponseEntity<List<ProtectedUserResponseDto>> getAllUsers() {
        List<User> users = this.userService.list();
        List<ProtectedUserResponseDto> protectedUserDtos = new ArrayList<>();
        for(User item:users){
            protectedUserDtos.add(
                new ProtectedUserResponseDto(
                    item.getName(),
                    item.getEmail(),
                    item.isActif(),
                    item.getPseudonyme(),
                    item.getAvatar(),
                    item.getRole().getName().toString()));
        }
       
        return ResponseEntity
                .status(HttpStatusCode.valueOf(200))
                .body(protectedUserDtos);
    }
   
}
