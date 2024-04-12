package fr.inote.inoteApi.crossCutting.security;

import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import fr.inote.inoteApi.service.UserService;

/**
 * Jwt filter, who perform JWT validation on a bearer token present in the HTTP
 * header. It is commonly used in conjunction with Spring Security and Spring
 * Boot to
 * authenticate and authorize users.
 * Here it inherits OncePerRequestFilter class, a special type of filter in
 * Spring that ensures the filter code is executed only once for each request.
 * This is
 * useful when working with Spring Security and wanting some authentication
 * actions to happen only once for a given request.
 */
@Service
public class JwtFilter extends OncePerRequestFilter {
    /*
     * HandlerExceptionResolver is an interface in Spring Framework that helps in
     * handling exceptions globally in a web application
     * We want our exceptions to be centralized in the ApplicationControllerAdvice
     * (@RestControllerAdvice) class we've set up. However, JWT filter exceptions
     * cannot be routed there, because the filter chain is upstream of the
     * DispatcherServlet.
     * HandlerExceptionResolver is used to retrieve an exception and forward it to
     * the DispatcherServlet, which will then forward it to the
     * RestControllerAdvice.
     */
    private final HandlerExceptionResolver handlerExceptionResolver;

    private final UserService utilisateurService;

    private final JwtServiceImpl jwtServiceImpl;

    @Autowired
    public JwtFilter(
            HandlerExceptionResolver handlerExceptionResolver,
            UserService utilisateurService,
            JwtServiceImpl jwtServiceImpl) {
        this.utilisateurService = utilisateurService;
        this.jwtServiceImpl = jwtServiceImpl;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * Apply filter procedure
     * doFilterInternal is a method inherited from the OncePerRequestFilter class in
     * the Spring Framework. This method is designed to ensure that the filter code
     * runs exactly once per request dispatch, regardless of the servlet container
     * being used.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) {

        String token;
        Jwt tokenInDatabase = null;
        String username = null;
        boolean isTokenExpired = true;

        try {

            // Récupération of token in header request
            final String authorization = request.getHeader("Authorization");

            // A token is present
            if (authorization != null && authorization.startsWith("Bearer ")) {
                // Token cleaning
                token = authorization.substring(7);

                // Token verification
                tokenInDatabase = jwtServiceImpl.findValidToken(token);

                // Get expiration token status
                isTokenExpired = jwtServiceImpl.isTokenExpired(token);

                // username recuperation
                username = jwtServiceImpl.extractUsername(token);
            }

            if (!isTokenExpired
                    && tokenInDatabase.getUser().getEmail().equals(username)
                    /*
                     * The SecurityContextHolder.getContext().getAuthentication() method is used in
                     * Spring Security to retrieve the currently authenticated user’s information
                     */
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                /*
                 * UserDetails interface represents information about a user. It contains
                 * details such as the user’s username, password, authorities (roles), and
                 * additional attributes
                 */
                UserDetails userDetails = utilisateurService.loadUserByUsername(username);

                /*
                 * UsernamePasswordAuthenticationToken is a concrete implementation of the
                 * Authentication interface used to represent the credentials of a user
                 * attempting to authenticate. It is commonly used in scenarios where you want
                 * to manually authenticate a user after their account is created or by other
                 * means not involving the traditional login form
                 */
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Validate the authentication
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            /*
             * Must be invoked once filter processing is complete. Invoking doFilter on the
             * FilterChain object instructs the web container to call the doFilter method of
             * the next filter in the chain, if any. If there is no longer a filter
             * associated with the resource, the web container processes the request and
             * generates a response. Once the web container has finished processing the
             * request and generating a response, it calls the doFilter method of the last
             * filter in the chain. This filter can then perform post-processing on the
             * request and response objects (e.g., save information, modify headers, etc.).
             * Finally, the filter's doFilter method is returned, enabling the web container
             * to return the response to the client
             */
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            /*
             * Transmitting error to the our RestControllerAdvice, who centralize exceptions
             */
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
