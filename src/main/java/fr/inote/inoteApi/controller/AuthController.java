package fr.inote.inoteApi.controller;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.AuthenticationDto;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


/**
 * Controller for routes related to user account
 */

/* Nota:
 * The @RestController annotation is a specialized version of the @Controller annotation in Spring MVC.
 * It combines the functionality of the @Controller and @ResponseBody annotations, which simplifies
 * the implementation of RESTful web services.
 * When a class is annotated with @RestController, the following points apply:
 * -> It acts as a controller, handling client requests.
 * -> The @ResponseBody annotation is automatically included, allowing the automatic serialization
 *    of the return object into the HttpResponse.
 */
@RestController
//@RequestMapping("/api/auth")
public class AuthController {

    /* Dependencies */
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

    /*DI*/
    @Autowired
    public AuthController(
            AuthenticationManager authenticationManager,
            UserServiceImpl userService,
            JwtServiceImpl jwtService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * Create user account
     *
     * @param userDto the user to register
     * @throws InoteExistingEmailException
     * @author atsuhiko Mochizuki
     * @date 28/03/2024
     */
    @PostMapping(Endpoint.REGISTER)
    public ResponseEntity<String> register(@RequestBody UserDto userDto) throws InoteExistingEmailException, InoteInvalidEmailException, InoteRoleNotFoundException, InoteInvalidPasswordFormatException {
        User userToRegister = User.builder()
                .email(userDto.username())
                .name(userDto.name())
                .password(userDto.password())
                .build();
        this.userService.register(userToRegister);

        return new ResponseEntity<>(MessagesEn.REGISTER_OK_MAIL_SENDED, HttpStatus.CREATED);
    }

    /**
     * Activate a user using the code provided on registration
     *
     * @param activationCode the activation code
     */
    @PostMapping(path = Endpoint.ACTIVATION)
    public ResponseEntity<String> activation(@RequestBody Map<String, String> activationCode) throws InoteValidationNotFoundException, InoteUserNotFoundException, InoteValidationExpiredException {
        this.userService.activation(activationCode);
        return new ResponseEntity<>(MessagesEn.ACTIVATION_OF_USER_OK, HttpStatus.OK);
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
     */
    @PostMapping(path = Endpoint.CHANGE_PASSWORD)
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> email) {
        try {
            this.userService.changePassword(email);
        } catch (UsernameNotFoundException ex) {
            return new ResponseEntity<>(MessagesEn.USER_ERROR_USER_NOT_FOUND, HttpStatus.BAD_REQUEST);
        } catch (InoteInvalidEmailException ex) {
            return new ResponseEntity<>(MessagesEn.EMAIL_ERROR_INVALID_EMAIL_FORMAT, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(MessagesEn.REGISTER_OK_MAIL_SENDED, HttpStatus.OK);
    }

    /**
     * Validate the new password with activation code provided on change password request
     * @param activationCode
     */
    @PostMapping(path = Endpoint.NEW_PASSWORD)
    public void newPassword(@RequestBody Map<String, String> activationCode) {
//        this.userService.newPassword(activationCode);
    }


}
