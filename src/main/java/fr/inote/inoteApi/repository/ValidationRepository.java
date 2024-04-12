package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.entity.Validation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ValidationRepository extends CrudRepository<Validation, Integer> {

    /**
     * Find by code optional.
     *
     * @param code the code
     * @return the optional
     * @author AtsuhikoMochizuki
     */
    Optional<Validation> findByCode(String code);

    /**
     * Deletes from the database all validations whose expiry date has passed at the
     * time passed in parameters
     *
     * @param now actualInstant of call method
     * @author AtsuhikoMochizuki
     */
    void deleteAllByExpirationBefore(Instant now);
}
