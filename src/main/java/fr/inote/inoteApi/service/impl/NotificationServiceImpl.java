package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.service.NotificationService;

import static fr.inote.inoteApi.crossCutting.constants.EmailAdress.NO_REPLY_EMAIL;

import static fr.inote.inoteApi.crossCutting.constants.RegexPatterns.REGEX_EMAIL_PATTERN;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    /* Dependencies */

    /*
     * The JavaMailSender interface is utilized in Java applications
     * for sending emails.
     * It is an extension of the MailSender interface, which adds
     * specialized JavaMail features like MIME message support
     */
    private final JavaMailSender javaMailSender;

    /* Dependencies injection */
    @Autowired
    public NotificationServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /* public methods */

    /**
     * Send validation by email.
     *
     * @param validation the validation
     * @author atsuhiko Mochizuki
     * @date 26-03-2024
     */
    @Override
    public void sendValidation_byEmail(Validation validation) throws MailException, InoteInvalidEmailException {
        this.sendEmail(
                NO_REPLY_EMAIL,
                validation.getUser().getEmail(),
                "Your activation code",
                String.format(
                        """
                                Inote notification service
                                %s

                                Hello %s, you have made on %s a request to create an account.
                                To activate your account, please enter the following activation code in the dedicated field:
                                activation code : %s

                                Inote wishes you a good day!
                                """,
                        EMAIL_SUBJECT_ACTIVATION_CODE,
                        validation.getUser().getName(),
                        validation.getCreation(),
                        validation.getCode()));

    }

    /* private methods */
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
