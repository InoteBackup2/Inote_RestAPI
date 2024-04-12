package fr.inote.inoteApi.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.inote.inoteApi.entity.User;

@Repository
public interface UserRepository extends CrudRepository<User, Integer> {

    /**
     * Find by email optional.
     *
     * @param email the email
     * @return the optional
     * @author AtsuhikoMochizuki
     */
    Optional<User> findByEmail(String email);
}
