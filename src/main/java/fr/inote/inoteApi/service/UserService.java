package fr.inote.inoteApi.service;

import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

public interface UserService extends UserDetailsService {

    /**
     * Create an user in database with:<br>
     * <ul>
     *     <li>validation of email format</li>
     *     <li>validation of password format</li>
     *     <li>checking is user is allready in database</li>
     *     <li>encryption of password with Bcrypt</li>
     * </ul>
     * then create a validation and save it in database
     * <p>
     *
     * @param user the user to register
     *             <p>
     * @return the user
     * @author Atsuhiko Mochizuki
     * @throws InoteExistingEmailException 
     * @date 26/03/2024
     */
    User register(User user) throws InoteUserException, InoteExistingEmailException;

    /**
     * Activate an user
     *
     * @param activation informations
     * @return
     */

    public User activation(Map<String, String> activation) throws InoteValidationNotFoundException, InoteValidationExpiredException, InoteUserNotFoundException;
}
