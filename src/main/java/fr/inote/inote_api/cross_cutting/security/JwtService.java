package fr.inote.inote_api.cross_cutting.security;

import fr.inote.inote_api.cross_cutting.exceptions.InoteExpiredRefreshTokenException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteJwtNotFoundException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteNotAuthenticatedUserException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteUserException;

import java.util.Map;

public interface JwtService {

    

    /**
     * Retrieve an token in database, actived and not expired
     *
     * @param value value of token to search in database
     * @return the JWT
     * @throws InoteNotAuthenticatedUserException
     */
    Jwt findValidToken(String value) throws InoteUserException, InoteNotAuthenticatedUserException;

    public Map<String, String> refreshConnectionWithRefreshTokenValue(String tokenValue)
            throws InoteJwtNotFoundException, InoteExpiredRefreshTokenException;

    /**
     * Signout of the user
     * <p>
     * Retrieve the authenticated user and his token with his email.
     * The token is deactivated and saved in database.
     */
    void signOut() throws InoteJwtNotFoundException;
}
