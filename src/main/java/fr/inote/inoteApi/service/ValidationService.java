package fr.inote.inoteApi.service;

import org.springframework.mail.MailException;

import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteMailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;

/**
 * The interface Validation service.
 * @author atsuhiko Mochizuki
 * @date 2024-03-26T15:30:00Z
 * <p>
 * Validation Service
 * A validation records:
 * - username
 * - instant of registration
 * - instant of expiration
 * - associate a random code to this request
 * This object is stored in the database. It ensures that the user has indeed
 * opened the e-mail (and therefore that the e-mail he has given is correct and
 * indeed his property).
 * When the user provides this code, the information sent is compared with the
 * information stored in the validation object, enabling the user to be
 * activated in the application.
 * Nota : Linking logins to e-mail addresses means it's easy for you to find
 * your
 * password, and for sites to contact you.
 * A confirmation code establishes two important facts.
 * -> First, that you haven't accidentally misspelled or mistyped your email
 * address when registering
 * -> Second, that you want to sign up for a site -- you're not being registered
 * there by someone else, perhaps as a prank. Using a code also helps ensure
 * that you're a real person and not a spambot -- a program that signs up to
 * sites in order to spread spam.
 */

public interface ValidationService {


    /**
     * Save validation in database
     *
     * @param user the user to save
     * @author atsuhiko Mochizuki
     * @throws InoteMailException 
     * @throws MailException 
     * @date 2024-03-26
     */
    Validation createAndSave(User user) throws InoteInvalidEmailException, MailException, InoteMailException;

    /**
     * Get the validation in database from code
     * @param code Sended code to email
     * @return the validation
     */
    Validation getValidationFromCode(String code) throws InoteValidationNotFoundException;

}
