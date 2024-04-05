package fr.inote.inoteApi.crossCutting.security;

import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;

public interface JwtService {
    long VALIDITY_TOKEN_TIME_IN_MINUTES = 1;

    /**
     * Retrieve an token in database, actived and not expired
     *
     * @param value value of token to search in database
     * @return the JWT
     */
    Jwt findValidToken(String value) throws InoteUserException;
}
