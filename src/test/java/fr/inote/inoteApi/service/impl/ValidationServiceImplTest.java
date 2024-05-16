package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteMailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.ValidationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import fr.inote.inoteApi.service.ValidationService;

/**
 * Unit tests of service ValidationServiceImplTest
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceImplTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* @Mock create and inject mocked instances of classes */
    @Mock
    private ValidationRepository mockedValidationRepository;
    @Mock
    private NotificationServiceImpl mockedNotificationService;

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * @InjectMocks instance class to be tested and automatically inject mock fields
     * Nota : if service is an interface, instanciate implementation withs mocks in
     * params
     */
    @InjectMocks
    private ValidationService validationService = new ValidationServiceImpl(mockedValidationRepository, mockedNotificationService);

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    private Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

    private User userForTest = User.builder()
            .email(REFERENCE_USER_EMAIL)
            .name(REFERENCE_USER_NAME)
            .password(REFERENCE_USER_PASSWORD)
            .role(roleForTest)
            .build();

    private Validation validationRef = Validation.builder()
            .code("123456")
            .user(this.userForTest)
            .creation(Instant.now())
            .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
            .build();

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void setUp() {}

    /* SERVICE UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Create validation ans save when user not exist in db and data are correct")
    void createAndSave_shouldCreateAnewValidationInDbAndSendCodeActivation_whenUserIsCorrectAndNotExistsInDatabase()
            throws InoteInvalidEmailException, MailException, InoteMailException {
        /* Arrange */
        when(this.mockedValidationRepository.save(any(Validation.class))).thenReturn(this.validationRef);
        doNothing().when(this.mockedNotificationService).sendValidation_byEmail(any(Validation.class));

        /* Act */
        Instant instantCreation = Instant.now();
        Validation validationTest = this.validationService.createAndSave(this.userForTest);

        /* Assert */
        assertThat(validationTest).isNotNull();
        assertThat(validationTest).isInstanceOf(Validation.class);

        assertThat(validationTest.getCode()).isNotNull();
        assertThat(Integer.parseInt(validationTest.getCode())).isLessThanOrEqualTo(999999);

        assertThat(validationTest.getCreation()).isAfter(instantCreation);
        assertThat(validationTest.getCreation()).isBefore(instantCreation.plusMillis(1000));

        assertThat(validationTest.getUser()).isEqualTo(this.userForTest);

        assertThat(validationTest.getActivation()).isNull();

        assertThat(validationTest.getExpiration()).isAfter(validationTest.getCreation().plusMillis(50));
        assertThat(validationTest.getExpiration())
                .isAfter(validationTest.getCreation().plus(5, ChronoUnit.MINUTES).minusMillis(50));

        /* Verify */
        verify(this.mockedValidationRepository, times(1)).save(any(Validation.class));
        verify(this.mockedNotificationService, times(1)).sendValidation_byEmail(any(Validation.class));
    }

    @Test
    @DisplayName("Search by activation code an existing validation in database")
    void getValidationFromCode_ShouldSuccess_WhenValidationExists() throws InoteValidationNotFoundException {
        /* Arrange */
        when(this.mockedValidationRepository.findByCode(any(String.class))).thenReturn(Optional.of(this.validationRef));

        /* Act & assert */
        assertThat(this.validationService.getValidationFromCode(this.validationRef.getCode()))
                .isEqualTo(this.validationRef);

        /* Verify */
        verify(this.mockedValidationRepository, times(1)).findByCode(any(String.class));
    }

    @Test
    @DisplayName("Search by activation code an non- existing validation in database")
    void getValidationFromCode_ShouldFail_WhenValidationNotExists() {

        /* Arrange */
        when(this.mockedValidationRepository.findByCode(any(String.class))).thenReturn(Optional.empty());

        /* Act */
        Throwable thrown = catchThrowable(
                () -> this.validationService.getValidationFromCode(this.validationRef.getCode()));

        /* Assert */
        assertThat(thrown).isInstanceOf(InoteValidationNotFoundException.class);

        /* Verify */
        verify(this.mockedValidationRepository, times(1)).findByCode(any(String.class));
    }

}