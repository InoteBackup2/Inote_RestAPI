package fr.inote.inoteApi.service;

import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.entity.User;

import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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
     * @throws InoteExistingEmailException
     * @author Atsuhiko Mochizuki
     * @throws InoteMailException 
     * @throws MailException 
     * @date 26/03/2024
     */
    User register(User user) throws InoteExistingEmailException, InoteInvalidEmailException, InoteRoleNotFoundException, InoteInvalidPasswordFormatException, MailException, InoteMailException;

    /**
     * Activate an user
     *
     * @param activation informations
     * @return
     */

    User activation(Map<String, String> activation) throws InoteValidationNotFoundException, InoteValidationExpiredException, InoteUserNotFoundException;

    /**
     * Ensure that password is 8 to 64 characters long and contains a mix of upper and lower case characters,
     * one numeric and one special character.
     *
     * @param password to be checked
     */
    void checkPasswordSecurityRequirements(String password) throws InoteInvalidPasswordFormatException;

    /**
     * Update the new password in database
     * After receiving his activation code by email, the user sends his new password, along with his email address and the code.
     * If the email corresponds to the validation referred to by the activation code,
     * the user's new password, if it meets security requirements,
     * is encoded and replaces the previous one.
     *
     * @param email containing user email
     */
    void newPassword(String email, String newPassword, String code) throws InoteValidationNotFoundException, UsernameNotFoundException, InoteInvalidPasswordFormatException;
}
