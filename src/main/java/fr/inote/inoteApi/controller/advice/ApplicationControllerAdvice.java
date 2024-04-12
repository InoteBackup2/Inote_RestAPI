package fr.inote.inoteApi.controller.advice;

import java.util.Map;

import fr.inote.inoteApi.crossCutting.exceptions.*;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.HttpStatus.*;


/**
 * Exception manager for controller layer
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

@Slf4j

/* The @RestControllerAdvice annotation in Spring is a specialization of @ControllerAdvice and
 * @ResponseBody. It is used for exception handling in Restful APIs and is commonly used to
 * handle exceptions in a centralized way for all controllers in an application.*/
@RestControllerAdvice
public class ApplicationControllerAdvice {

    /**
     * Exception handler for problems with JWT (malformed and bad signature)
     * <p>
     * Nota : The @ExceptionHandler annotation allows you to define methods that
     * handle specific exceptions thrown by your controller methods.
     * These methods can be defined either within the same controller
     * or in a separate class annotated with @ControllerAdvice for global exception
     * handling.
     * The @MalformedJwtException is a specific exception that occurs when a JWT
     * (JSON Web Token)
     * string contains an invalid number of period characters.
     * The SignatureException occurs when there is a problem with the signature of a
     * JSON Web Token.
     * <p>
     * Annotation @ResponseStatus is used in Spring applications to mark a method or exception
     * class with the HTTP status code and reason message that should be returned.
     * When the handler method is invoked, the status code is applied to the HTTP response,
     * or if a specific exception is thrown.
     *
     * @param exception
     * @return return a JSON object with 401 status code
     */
    @ExceptionHandler(value = {MalformedJwtException.class, SignatureException.class})
    @ResponseStatus(UNAUTHORIZED) // return 401 Unauthorized status http status
    public @ResponseBody ProblemDetail badCredentialsException(final Exception exception) {

        log.error(exception.getMessage(), exception);

        // ProblemDetails is proposed by Spring for return exceptions
        return ProblemDetail.forStatusAndDetail(UNAUTHORIZED, "Invalid token");
    }

    /**
     * Invalid credentials manager
     * <p>
     * Nota : The BadCredentialsException is a subclass of
     * AuthenticationException in Spring Security. This exception is thrown
     * when invalid authentication credentials are provided.
     *
     * @param exception
     * @return an http response with json object with 401 status code
     * when credentials are invalid
     */
    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(value = BadCredentialsException.class)
    public @ResponseBody ProblemDetail badCredentialsException(final BadCredentialsException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(UNAUTHORIZED, "Invalid credentials");
        problemDetail.setProperty("Error", "Impossible authentication");

        return problemDetail;
    }

    @ResponseStatus(NOT_ACCEPTABLE)
    @ExceptionHandler(value = InoteExistingEmailException.class)
    public @ResponseBody ProblemDetail InoteExistingEmailException(final InoteExistingEmailException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_ACCEPTABLE, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteValidationNotFoundException.class)
    public @ResponseBody ProblemDetail InoteValidationNotFoundException(final InoteValidationNotFoundException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(NOT_ACCEPTABLE)
    @ExceptionHandler(value = InoteValidationExpiredException.class)
    public @ResponseBody ProblemDetail InoteValidationExpiredException(final InoteValidationExpiredException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_ACCEPTABLE, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteUserNotFoundException.class)
    public @ResponseBody ProblemDetail InoteUserNotFoundException(final InoteUserNotFoundException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteInvalidEmailException.class)
    public @ResponseBody ProblemDetail InoteInvalidEmailException(final InoteInvalidEmailException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteInvalidPasswordFormatException.class)
    public @ResponseBody ProblemDetail InoteInvalidPasswordFormatException(final InoteInvalidPasswordFormatException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteRoleNotFoundException.class)
    public @ResponseBody ProblemDetail InoteRoleNotFoundException(final InoteRoleNotFoundException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(value = InoteJwtNotFoundException.class)
    public @ResponseBody ProblemDetail InoteJwtNotFoundException(final InoteJwtNotFoundException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }

    @ResponseStatus(NOT_ACCEPTABLE)
    @ExceptionHandler(value = InoteEmptyMessageCommentException.class)
    public @ResponseBody ProblemDetail InoteEmptyMessageCommentException(final InoteEmptyMessageCommentException exception) {

        log.error(exception.getMessage(), exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_ACCEPTABLE, exception.getMessage());
        problemDetail.setProperty("Error", exception.getMessage());

        return problemDetail;
    }



    /**
     * Default Exception manager
     *
     * @return Http response with 401 code, with json object body
     */
    @ResponseStatus(UNAUTHORIZED)
    @ExceptionHandler(value = Exception.class)
    public Map<String, String> exceptionsHandler() {
        return Map.of("Error", "Detected anomaly");
    }
}
