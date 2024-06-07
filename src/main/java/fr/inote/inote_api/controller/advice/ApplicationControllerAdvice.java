package fr.inote.inote_api.controller.advice;

import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.inote.inote_api.cross_cutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteExistingEmailException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteInvalidEmailException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteInvalidPasswordFormatException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteJwtNotFoundException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteNotAuthenticatedUserException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteRoleNotFoundException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteUserNotFoundException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteValidationExpiredException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteValidationNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Locale;

import org.apache.tomcat.websocket.AuthenticationException;

/**
 * Centralized exception manager for controller layer
 * 
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

@Slf4j // For output errors in console
@RestControllerAdvice // Exception Centralized manager
public class ApplicationControllerAdvice {

    private final MessageSource messageSource;

    public ApplicationControllerAdvice(MessageSource messageSource){
        this.messageSource = messageSource;
    }

   /**
     * Handle exception when jwt is malformed
     * 
     * @param ex exception
     * @return 401 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = MalformedJwtException.class)
    private ProblemDetail MalformedJwtExceptionHandler(MalformedJwtException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when problem with credentials signature append
     * 
     * @param ex exception
     * @return 401 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = SignatureException.class)
    private ProblemDetail SignatureExceptionHandler(SignatureException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when bad credentials
     * 
     * @param ex exception
     * @return 401 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = BadCredentialsException.class)
    private ProblemDetail BadCredentialsExceptionHandler(BadCredentialsException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when authentication object being invalid for whatever reason.
     * 
     * @param ex exception
     * @return 401 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = AuthenticationException.class)
    private ProblemDetail AuthenticationExceptionHandler(AuthenticationException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, ex.getLocalizedMessage());
    }

    
    
    /**
     * Handle exception when email is unknow in database
     * 
     * @param ex exception
     * @return 406 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteExistingEmailException.class)
    private ProblemDetail InoteExistingEmailExceptionHandler(Exception ex, Locale locale) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(NOT_ACCEPTABLE, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when validation is unknow in database
     * 
     * @param ex exception
     * @return 404 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteValidationNotFoundException.class)
    private ProblemDetail InoteValidationNotFoundExceptionHandler(Exception ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when validation is expired
     * 
     * @param ex exception
     * @return 404 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteValidationExpiredException.class)
    private ProblemDetail InoteValidationExpiredExceptionHandler(Exception ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when user is unknow in database
     * 
     * @param ex exception
     * @return 404 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteUserNotFoundException.class)
    private ProblemDetail InoteUserNotFoundExceptionHandler(Exception ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when email is malformed
     * 
     * @param ex exception
     * @return 400 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteInvalidEmailException.class)
    private ProblemDetail InoteInvalidEmailExceptiondeHandler(InoteInvalidEmailException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(BAD_REQUEST, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when password is invalid
     * 
     * @param ex exception
     * @return 400 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteInvalidPasswordFormatException.class)
    private ProblemDetail InoteInvalidPasswordFormatExceptionHandler(InoteInvalidPasswordFormatException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(BAD_REQUEST, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when role is unknow in database
     * 
     * @param ex exception
     * @return 400 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteRoleNotFoundException.class)
    private ProblemDetail InoteRoleNotFoundExceptionHandler(InoteRoleNotFoundException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(BAD_REQUEST, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when token is unknow in database
     * 
     * @param ex exception
     * @return 400 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteJwtNotFoundException.class)
    private ProblemDetail InoteJwtNotFoundException(InoteJwtNotFoundException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(BAD_REQUEST, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when comment message is empty
     * 
     * @param ex exception
     * @return 406 status code and exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = InoteEmptyMessageCommentException.class)
    private ProblemDetail InoteEmptyMessageCommentExceptionHandler(InoteEmptyMessageCommentException ex) {

        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(NOT_ACCEPTABLE, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when user authentication fail
     * 
     * @param ex
     * @return 401 status code and exception cause
     * 
     * @author atsuhikoMochizuki
     * @since 2024-05-28
     */
    @ExceptionHandler(value = InoteNotAuthenticatedUserException.class)
    private ProblemDetail InoteNotAuthenticatedUserExceptionHandler(InoteNotAuthenticatedUserException ex) {
        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, ex.getLocalizedMessage());
    }

    /**
     * Handle exception when Jwt is expired
     * @param ex
     * @return 401 status code and exception cause
     * 
     * @author atsuhikoMochizuki
     * @since 2024-05-30
     */
    @ExceptionHandler(value = ExpiredJwtException.class)
    private ProblemDetail InoteExpiredJwtExceptionHandler(ExpiredJwtException ex, Locale locale) {
        log.error(ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(
            UNAUTHORIZED,
            messageSource.getMessage(
                "miscelleanous.EXPIRED_TOKEN",
                null,
                locale));
    }
    
    /**
     * Default exception handler
     * 
     * @param ex Default type exception
     * @return a 400 status code with exception cause
     * @author atsuhikoMochizuki
     * @date 19-05-2024
     */
    @ExceptionHandler(value = Exception.class)
    public ProblemDetail inoteDefaultExceptionHandler(Exception ex) {

        // Loging error in console
        log.error(ex.getMessage(), ex);

        return ProblemDetail
                .forStatusAndDetail(
                        // return status code
                        BAD_REQUEST,
                        // return reason
                        ex.getLocalizedMessage());
    }
}
