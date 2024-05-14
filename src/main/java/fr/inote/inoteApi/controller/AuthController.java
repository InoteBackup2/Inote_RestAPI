package fr.inote.inoteApi.controller;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.*;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.BEARER;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;

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
public class AuthController {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * The AuthenticationManager in Spring Security is responsible for
     * authenticating user credentials. It provides methods to authenticate user
     * credentials and determine if the user is authorized to access the requested
     * resource. Here’s how it works:
     *
     * 1- Implement the AuthenticationManager interface or use the provided
     * ProviderManager implementation.
     *
     * 2- In your custom implementation or configuration, configure one or more
     * AuthenticationProvider instances. An AuthenticationProvider is responsible
     * for authenticating a specific type of credential (e.g., username/password,
     * OAuth2, LDAP, etc.).
     *
     * 3- The AuthenticationManager delegates the authentication process to the
     * appropriate AuthenticationProvider based on the credential type.
     *
     * 4- If the authentication is successful, the AuthenticationManager creates an
     * Authentication object containing the authenticated user’s information.
     * Otherwise, it throws an appropriate exception (e.g., BadCredentialsException,
     * DisabledException, LockedException).
     */
    private final AuthenticationManager authenticationManager;
    private final UserServiceImpl userService;
    private final JwtServiceImpl jwtService;

    @Autowired
    public AuthController(
            AuthenticationManager authenticationManager,
            UserServiceImpl userService,
            JwtServiceImpl jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /* PUBLIC METHODS */
    /* ============================================================ */

    /**
     * Create user account
     * 
     * @param userDto
     * @return ResponseEntity<String> response
     * @throws InoteExistingEmailException
     * @throws InoteInvalidEmailException
     * @throws InoteRoleNotFoundException
     * @throws InoteInvalidPasswordFormatException
     */
    @PostMapping(path = Endpoint.REGISTER)
    public ResponseEntity<Map<String, String>> register(@RequestBody UserDto userDto) {
        Map<String, String> responseMsg = new HashMap<>();

        User userToRegister = User.builder()
                .email(userDto.username())
                .name(userDto.name())
                .password(userDto.password())
                .build();
        try {
            this.userService.register(userToRegister);
        } catch (InoteMailException | InoteExistingEmailException | InoteInvalidEmailException
                | InoteRoleNotFoundException
                | InoteInvalidPasswordFormatException ex) {
            responseMsg.put("msg", ex.getMessage());
            return new ResponseEntity<>(responseMsg, HttpStatus.BAD_REQUEST);
        }
        responseMsg.put("msg", MessagesEn.ACTIVATION_NEED_ACTIVATION);


        return new ResponseEntity<>(responseMsg, HttpStatus.CREATED);


    }

    /**
     * Activate a user using the code provided on registration
     * 
     * @param activationCode
     * @return
     * @throws InoteValidationNotFoundException
     * @throws InoteUserNotFoundException
     * @throws InoteValidationExpiredException
     */
    @PostMapping(path = Endpoint.ACTIVATION)
    public ResponseEntity<Map<String, String>> activation(@RequestBody Map<String, String> activationCode) {
        Map<String, String> responseMsg = new HashMap<>();

        try {
            this.userService.activation(activationCode);
        } catch (InoteValidationNotFoundException | InoteValidationExpiredException | InoteUserNotFoundException ex) {
            responseMsg.put("msg", ex.getMessage());
            responseMsg.put("msg", ex.getMessage());
            return new ResponseEntity<>(responseMsg, HttpStatus.BAD_REQUEST);
        }
        responseMsg.put("msg", MessagesEn.ACTIVATION_OF_USER_OK);
        return new ResponseEntity<>(responseMsg, HttpStatus.OK);
        
    }

    /**
     * Authenticate an user and give him a JWT token for secured actions in app
     *
     * @param authenticationDto
     * @return a JWT token if user is authenticated or null
     */
    @PostMapping(path = Endpoint.SIGN_IN)
    public ResponseEntity<Map<String, String>> signIn(@NotNull @RequestBody AuthenticationDto authenticationDto) {
        final Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationDto.username(),
                        authenticationDto.password()));

        if (authenticate.isAuthenticated()) {
            return new ResponseEntity<>(this.jwtService.generate(authenticationDto.username()), HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Send a password change request
     *
     * @param email
     * @throws InoteMailException
     * @throws MailException
     */
    @PostMapping(path = Endpoint.CHANGE_PASSWORD)
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> email)
            throws MailException, InoteMailException {
        try {
            this.userService.changePassword(email);
        } catch (UsernameNotFoundException | InoteInvalidEmailException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(MessagesEn.ACTIVATION_NEED_ACTIVATION, HttpStatus.OK);
    }

    /**
     * Validate the new password with activation code provided on change password
     * request
     *
     * @param newPasswordDto informationsSendedInBodeRequest
     */
    @PostMapping(path = Endpoint.NEW_PASSWORD)
    public ResponseEntity<String> newPassword(@RequestBody NewPasswordDto newPasswordDto) {
        try {
            this.userService.newPassword(
                    newPasswordDto.email(),
                    newPasswordDto.password(),
                    newPasswordDto.code());
        } catch (UsernameNotFoundException | InoteValidationNotFoundException
                | InoteInvalidPasswordFormatException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(MessagesEn.NEW_PASSWORD_SUCCESS, HttpStatus.OK);
    }

    /**
     * Refresh connection with refresh token
     *
     * @param refreshConnectionDto the value of refresh token
     * @return the value of new bearer and refresh token
     */
    @PostMapping(path = Endpoint.REFRESH_TOKEN)
    public @ResponseBody ResponseEntity<SignInResponseDto> refreshConnectionWithRefreshTokenValue(
            @RequestBody RefreshConnectionDto refreshConnectionDto) {

        Map<String, String> response;

        try {
            response = this.jwtService.refreshConnectionWithRefreshTokenValue(refreshConnectionDto.refresh());
        } catch (InoteJwtNotFoundException | InoteExpiredRefreshTokenException ex) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        SignInResponseDto signInResponseDto = new SignInResponseDto(
                response.get(BEARER),
                response.get(REFRESH));
        return new ResponseEntity<>(signInResponseDto, HttpStatus.CREATED);
    }

    /**
     * user signout
     */
    @PostMapping(path = Endpoint.SIGN_OUT)
    public ResponseEntity<String> signOut() {
        try {
            this.jwtService.signOut();
        } catch (InoteJwtNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(MessagesEn.USER_SIGNOUT_SUCCESS, HttpStatus.OK);
    }

    /**
     * Get informations of current connected user
     * 
     * @param user
     * @return ResponseEntity<Map<String, PublicUserDto>>
     * @throws InoteUserNotFoundException
     * 
     * @author AtsuhikoMochizuki
     * @date 14-05-2024
     */
    @GetMapping(path = Endpoint.GET_CURRENT_USER)
    public ResponseEntity<Map<String, PublicUserDto>> getCurrentUser(@AuthenticationPrincipal User user)
            throws InoteUserNotFoundException {
        Map<String, PublicUserDto> responseMsg = new HashMap<>();
        if (user == null) {
            throw new InoteUserNotFoundException();
        }
        PublicUserDto publicUserDto = new PublicUserDto(user.getName(), user.getUsername(), null, user.isActif(),
                user.getRole().getName().toString());

        responseMsg.put("data", publicUserDto);
        return new ResponseEntity<>(responseMsg, HttpStatus.OK);
    }
}
