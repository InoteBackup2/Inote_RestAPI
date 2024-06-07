package fr.inote.inote_api.service.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.inote.inote_api.cross_cutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static fr.inote.inote_api.cross_cutting.constants.RegexPatterns.REGEX_EMAIL_PATTERN;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import fr.inote.inote_api.cross_cutting.exceptions.InoteInvalidEmailException;
import fr.inote.inote_api.cross_cutting.exceptions.InoteMailException;
import fr.inote.inote_api.entity.Validation;
import fr.inote.inote_api.service.NotificationService;

/**
 * The Service NotificationServiceImpl
 * 
 * @author Atsuhiko Mochizuki
 * @date 11/04/2024
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */

    /*
     * The JavaMailSender interface is utilized in Java applications
     * for sending emails.
     * It is an extension of the MailSender interface, which adds
     * specialized JavaMail features like MIME message support
     */
    private final JavaMailSender javaMailSender;

    @Value("${inote.backend.mail.notreply}")
    private String NOT_REPLY_ADRESS_MAIL;
    
    public NotificationServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /* PUBLIC METHODS */
    /* ============================================================ */

    /**
     * Send validation by email.
     *
     * @param validation the validation
     * @author atsuhiko Mochizuki
     * @throws InoteMailException
     * @date 26-03-2024
     */
    @Override
    public void sendValidation_byEmail(Validation validation)
            throws MailException, InoteInvalidEmailException, InoteMailException {
        try {

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss.SSS");
            String formattedDateTime = validation.getCreation().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    .format(dateFormatter);

            this.sendEmail(
                    NOT_REPLY_ADRESS_MAIL,
                    validation.getUser().getEmail(),
                    "Your activation code",
                    String.format(
                            """
                                    Inote notification service
                                    %s

                                    Hello %s, you have made on %s a request that requires an activation code.
                                    Please enter the following code in asked field:
                                    activation code : %s

                                    Inote wishes you a good day!
                                    """,
                            EMAIL_SUBJECT_ACTIVATION_CODE,
                            validation.getUser().getName(),
                            formattedDateTime,
                            validation.getCode()));
        } catch (MailException e) {
            throw new InoteMailException();
        }
    }

    /* PRIVATE METHODS */
    /* ============================================================ */
    /**
     * Send an email
     * 
     * @param from
     * @param to
     * @param subject
     * @param content
     * @throws MailException
     * @throws InoteInvalidEmailException
     * 
     * @author Atsuhiko Mochizuki
     * @date 11/04/2024
     */
    private void sendEmail(String from,
            String to,
            String subject,
            String content) throws MailException, InoteInvalidEmailException {

        Pattern compiledPattern;
        Matcher matcher;

        // Email format checking
        compiledPattern = Pattern.compile(REGEX_EMAIL_PATTERN);
        matcher = compiledPattern.matcher(from);
        if (!matcher.matches()) {
            throw new InoteInvalidEmailException();
        }

        matcher = compiledPattern.matcher(to);
        if (!matcher.matches()) {
            throw new InoteInvalidEmailException();
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(content);

        this.javaMailSender.send(mail);
    }
}
