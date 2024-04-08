package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.ValidationRepository;
import fr.inote.inoteApi.service.NotificationService;
import fr.inote.inoteApi.service.ValidationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Transactional
@Service
public class ValidationServiceImpl implements ValidationService {

    /*Dependencies*/
    private final ValidationRepository validationRepository;
    private final NotificationService notificationService;

    /*Dependencies injection*/
    @Autowired
    public ValidationServiceImpl(ValidationRepository validationRepository, NotificationService notificationService) {
        this.validationRepository = validationRepository;
        this.notificationService = notificationService;
    }

    /*public methods*/

    /**
     * Create and save validation in database
     *
     * @param user the user to save
     * @author atsuhiko Mochizuki
     * @date 2024-03-26
     */
    @Override
    public Validation createAndSave(User user) throws InoteInvalidEmailException {
        Validation validation = Validation.builder()
                .user(user)
                .creation(Instant.now())
                .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
                .code(String.format("%06d", new Random().nextInt(999999)))
                .build();
        this.validationRepository.save(validation);

        this.notificationService.sendValidation_byEmail(validation);

        return validation;

    }

    /**
     * Get the validation in database from code
     *
     * @param code Sended code to email
     * @return the validation
     */
    @Override
    public Validation getValidationFromCode(String code) throws InoteValidationNotFoundException {
        return this.validationRepository.findByCode(code)
                .orElseThrow(InoteValidationNotFoundException::new);
    }
}
