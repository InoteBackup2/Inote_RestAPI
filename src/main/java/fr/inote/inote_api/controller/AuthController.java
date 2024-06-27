package fr.inote.inote_api.controller;

import fr.inote.inote_api.cross_cutting.constants.Endpoint;
import fr.inote.inote_api.cross_cutting.exceptions.*;
import fr.inote.inote_api.cross_cutting.constants.MessagesEn;
import fr.inote.inote_api.cross_cutting.security.impl.JwtServiceImpl;
import fr.inote.inote_api.dto.*;
import fr.inote.inote_api.entity.User;
import fr.inote.inote_api.service.impl.UserServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static fr.inote.inote_api.cross_cutting.constants.HttpRequestBody.BEARER;
import static fr.inote.inote_api.cross_cutting.constants.HttpRequestBody.REFRESH;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

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
     * @param registerRequestDto 
     * @return ResponseEntity<String> Response entity (http gestion facilities) that
     *         contains type of data in response body
     * @throws InoteExistingEmailException
     * @throws InoteInvalidEmailException
     * @throws InoteRoleNotFoundException
     * @throws InoteInvalidPasswordFormatException
     * 
     * @author atsuhikoMochizuki
     * @throws InoteMailException
     * @throws MailException
     * @since 19/05/2024
     */

    @PostMapping(path = Endpoint.REGISTER)
    public ResponseEntity<String> register(@RequestBody RegisterRequestDto registerRequestDto)
            throws MailException, InoteExistingEmailException, InoteInvalidEmailException, InoteRoleNotFoundException,
            InoteInvalidPasswordFormatException, InoteMailException {

        User userToRegister = User.builder()
                .email(registerRequestDto.username())
                .name(registerRequestDto.pseudo())
                .pseudonyme(registerRequestDto.pseudo())
                .password(registerRequestDto.password())
                .build();

        this.userService.register(userToRegister);

        return ResponseEntity
                .status(HttpStatusCode.valueOf(201))
                .body(MessagesEn.ACTIVATION_NEED_ACTIVATION);
    }

    /**
     * Activate a user using the code provided on registration
     * 
     * @param activationCode a String for activation code provided by email on
     *                       registration
     * @return a response entity that contains status code and a msg that concerns
     *         request
     * 
     * @throws InoteValidationNotFoundException
     * @throws InoteUserNotFoundException
     * @throws InoteValidationExpiredException
     * 
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @PostMapping(path = Endpoint.ACTIVATION)
    public ResponseEntity<String> activation(@RequestBody ActivationRequestDto activationRequestDto)
            throws InoteValidationNotFoundException, InoteValidationExpiredException, InoteUserNotFoundException {

        this.userService.activation(activationRequestDto.code());

        return ResponseEntity
                .status(OK)
                .body(MessagesEn.ACTIVATION_OF_USER_OK);

    }

    /**
     * Authenticate an user and give him a JWT token for secured actions in app
     *
     * @param signInRequestDto that contains required user informations
     * @return a JWT token if user is authenticated or null
     * 
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @PostMapping(path = Endpoint.SIGN_IN)
    public ResponseEntity<SignInResponseDto> signIn(@RequestBody SignInRequestDto signInRequestDto) throws AuthenticationException{
        
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInRequestDto.username(),
                        signInRequestDto.password()));
        UserDetails userDetails = (UserDetails) authenticate.getPrincipal();
        Map<String, String> map = this.jwtService.generate(userDetails.getUsername());
        SignInResponseDto signInReponseDto = new SignInResponseDto(map.get(BEARER), map.get(REFRESH));

        return ResponseEntity
                .status(OK)
                .body(signInReponseDto);
    }

    /**
     * Change password
     *
     * @param email of user concerned
     * @throws InoteMailException
     * @throws MailException
     * 
     * @author atsuhikoMochizuki
     * @throws InoteInvalidEmailException
     * @date 19-05-2024
     */
    @PostMapping(path = Endpoint.CHANGE_PASSWORD)
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequestDto changePasswordRequestDto)
            throws UsernameNotFoundException, MailException, InoteMailException, InoteInvalidEmailException {
        this.userService.changePassword(changePasswordRequestDto.email());

        return ResponseEntity
                .status(OK)
                .body(MessagesEn.ACTIVATION_NEED_ACTIVATION);
    }

    /**
     * Validate the new password with activation code provided on change password
     * request
     *
     * @param requiredInfos NewPasswordDto that contains email, code and
     *                      passwordToChange
     * @return status code whith associated comment
     * 
     * @author atsuhikoMochizuki
     * @throws InoteInvalidPasswordFormatException
     * @throws InoteValidationNotFoundException
     * @throws UsernameNotFoundException
     * @date 19-05-2024
     */
    @PostMapping(path = Endpoint.NEW_PASSWORD)
    public ResponseEntity<String> newPassword(@RequestBody NewPasswordRequestDto newPasswordRequestDto)
            throws UsernameNotFoundException, InoteValidationNotFoundException, InoteInvalidPasswordFormatException {

        this.userService.newPassword(
                newPasswordRequestDto.email(),
                newPasswordRequestDto.password(),
                newPasswordRequestDto.code());

        return ResponseEntity
                .status(OK)
                .body(MessagesEn.NEW_PASSWORD_SUCCESS);
    }

    /**
     * Refresh connection with refresh token
     *
     * @param refreshConnectionDto the value of refresh token
     * @return the value of new bearer and refresh token
     * @throws InoteExpiredRefreshTokenException
     * @throws InoteJwtNotFoundException
     */

    /**
     * 
     * @param refreshRequestDto RefreshConnectionDto
     * @return
     * @throws InoteJwtNotFoundException
     * @throws InoteExpiredRefreshTokenException
     */ 
    @PostMapping(path = Endpoint.REFRESH_TOKEN)
    public ResponseEntity<SignInResponseDto> refreshConnectionWithRefreshTokenValue(
        @RequestBody RefreshRequestDto refreshRequestDto)
            throws InoteJwtNotFoundException, InoteExpiredRefreshTokenException {

        Map<String, String> response;

        response = this.jwtService.refreshConnectionWithRefreshTokenValue(refreshRequestDto.refresh());

        SignInResponseDto signInResponseDto = new SignInResponseDto(
                response.get(BEARER),
                response.get(REFRESH));
        return ResponseEntity
                .status(CREATED)
                .body(signInResponseDto);
    }

    /**
     * user signout
     * @throws InoteJwtNotFoundException 
     */

     /**
      * User sign out
      *
      * @return status code with associated comment
      * @throws InoteJwtNotFoundException
      * @author atsuhikoMochizuki
      * @date 19-05-2024
      */
    @PostMapping(path = Endpoint.SIGN_OUT)
    public ResponseEntity<String> signOut() throws InoteJwtNotFoundException {
        
        this.jwtService.signOut();
        
        return ResponseEntity
            .status(OK)
            .body(MessagesEn.USER_SIGNOUT_SUCCESS);
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
    public ResponseEntity<PublicUserRequestDto> getCurrentUser(@AuthenticationPrincipal User user)
            throws InoteUserNotFoundException {
        if (user == null) {
            throw new InoteUserNotFoundException();
        }
        PublicUserRequestDto publicUserDto = new PublicUserRequestDto(user.getName(), user.getUsername(), null, user.isActif(),
                user.getRole().getName().toString());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(publicUserDto);
    }
}
