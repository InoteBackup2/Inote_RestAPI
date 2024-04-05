package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.security.Jwt;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * The interface Jwt repository.
 * @author atsuhiko Mochizuki
 */
@Repository
public interface JwtRepository extends CrudRepository<Jwt, Integer> {
    Optional<Jwt> findByContentValueAndDeactivatedAndExpired(String value, boolean deactivated, boolean expired);

    @Query("FROM Jwt j WHERE j.expired = :expired AND j.deactivated = :deactivated AND j.user.email = :email")
    Optional<Jwt> findTokenWithEmailAndStatusToken(String email, boolean deactivated, boolean expired);

    @Query("FROM Jwt j WHERE j.user.email = :email")
    Stream<Jwt> findJwtWithUserEmail(String email);

    @Query("FROM Jwt j WHERE j.refreshToken.contentValue = :contentValue")
    Optional<Jwt> findJwtWithRefreshTokenValue(String contentValue);

    void deleteAllByExpiredAndDeactivated(boolean expired, boolean deactivated);
}