package fr.inote.inoteApi.crossCutting.security;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;

/**
 * Refresh Token
 * A refresh token is a mechanism used in authentication protocols to extend the
 * lifetime of an access token
 * without having to re-authenticate the user.
 * Unlike access tokens, which have a limited lifespan, generally ranging from a
 * few minutes to a few hours,
 * refresh tokens have a much longer lifespan, from a few days to several months
 * or even years.
 * When an access token expires, instead of asking the user to reconnect, the
 * application can use the
 * refresh token to obtain a new access token from the authorization server.
 * This maintains the user's
 * access to a protected resource without requiring frequent reconnection.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class RefreshToken{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private boolean expirationStatus;
    private String contentValue;
    private Instant creationDate;
    private Instant expirationDate;
}

