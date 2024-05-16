package fr.inote.inoteApi.crossCutting.security.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class JwtServiceImpl_IT {

    @Test
    @DisplayName("Desactivate of a token")
    @Disabled
    void disableTokens_shouldSuccess_whenTokenIsWellFormed(){
        // Must be tested with database access
        Assertions.fail("not yet implemented!");
    }

    @Test
    @DisplayName("User signout")
    @Disabled
    void signOut_shouldSuccess_whenUserIsConnected(){
        // Must be tested with database access
        Assertions.fail("not yet implemented!");
    }

    @Test
    @DisplayName("Remove periodically inactive tokens in db")
    @Disabled
    void removeUselessJwt_shouldSuccess(){
        // Must be tested with database access
        Assertions.fail("not yet implemented!");
    }
}
