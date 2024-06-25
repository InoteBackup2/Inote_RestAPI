package fr.inote.inote_api.service;

import fr.inote.inote_api.cross_cutting.exceptions.InoteInvalidEmailException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteMailException;
import fr.inote.inote_api.entity.Validation;
import org.springframework.mail.MailException;

/**
 * The interface Notification service.
 * @author atsuhiko Mochizuki
 * @date 26-03-2024
 */
public interface NotificationService {
    /**
     * Send validation by email.
     * @author atsuhiko Mochizuki
     *
     * @date 26-03-2024
     * @param validation the validation
     * @throws InoteMailException 
     */
    void sendValidation_byEmail(Validation validation) throws MailException, InoteInvalidEmailException, InoteMailException;

}
