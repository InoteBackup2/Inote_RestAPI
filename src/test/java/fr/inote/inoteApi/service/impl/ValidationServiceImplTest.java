package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailFormat;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.ValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

/**
 * The type Validation service impl test.
 *
 * @author atsuhiko Mochizuki
 * @date 27/03/2024
 */
@ActiveProfiles("test")
@Tag("Service tests")
@ExtendWith(MockitoExtension.class)
class ValidationServiceImplTest {

    /* Dependencies and injection*/
    @Mock
    private ValidationRepository mockedValidationRepository;
    @Mock
    private NotificationServiceImpl mockedNotificationService;

    @InjectMocks
    private ValidationServiceImpl validationService;

    /* attributes*/
    private Validation validationRef;

    private User userForTest;

    @BeforeEach
    public void init() {
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        this.userForTest = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();

        this.validationRef = Validation.builder()
                .code("123456")
                .user(this.userForTest)
                .creation(Instant.now())
                .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
    }

    @Test
    @DisplayName("Create validation ans save when user not exist in db and data are correct")
    void createAndSave_shouldCreateAnewValidationInDbAndSendCodeActivation_whenUserIsCorrectAndNotExistsInDatabase() throws InoteUserException, InoteInvalidEmailFormat {
        // ARRANGE
        when(this.mockedValidationRepository.save(any(Validation.class))).thenReturn(this.validationRef);
        doNothing().when(this.mockedNotificationService).sendValidation_byEmail(any(Validation.class));


        // ACT
        Instant instantCreation = Instant.now();
        Validation validationTest = this.validationService.createAndSave(this.userForTest);

        // ASSERT
        assertThat(validationTest).isNotNull();
        assertThat(validationTest).isInstanceOf(Validation.class);

        assertThat(validationTest.getCode()).isNotNull();
        assertThat(Integer.parseInt(validationTest.getCode())).isLessThanOrEqualTo(999999);

        assertThat(validationTest.getCreation()).isAfter(instantCreation);
        assertThat(validationTest.getCreation()).isBefore(instantCreation.plusMillis(1000));

        assertThat(validationTest.getUser()).isEqualTo(this.userForTest);

        assertThat(validationTest.getActivation()).isNull();

        assertThat(validationTest.getExpiration()).isAfter(validationTest.getCreation().plusMillis(50));
        assertThat(validationTest.getExpiration()).isAfter(validationTest.getCreation().plus(5, ChronoUnit.MINUTES).minusMillis(50));

        verify(this.mockedValidationRepository, times(1)).save(any(Validation.class));
        verify(this.mockedNotificationService, times(1)).sendValidation_byEmail(any(Validation.class));
    }


    @Test
    @DisplayName("Search by activation code an existing validation in database")
    void getValidationFromCode_ShouldSuccess_WhenValidationExists() throws InoteValidationNotFoundException {
        when(this.mockedValidationRepository.findByCode(any(String.class))).thenReturn(Optional.of(this.validationRef));

        assertThat(this.validationService.getValidationFromCode(this.validationRef.getCode())).isEqualTo(this.validationRef);

        verify(this.mockedValidationRepository, times(1)).findByCode(any(String.class));
    }

    @Test
    @DisplayName("Search by activation code an non- existing validation in database")
    void getValidationFromCode_ShouldFail_WhenValidationNotExists() {
        when(this.mockedValidationRepository.findByCode(any(String.class))).thenReturn(Optional.empty());

        Throwable thrown = catchThrowable(() -> this.validationService.getValidationFromCode(this.validationRef.getCode()));

        assertThat(thrown).isInstanceOf(InoteValidationNotFoundException.class);

        verify(this.mockedValidationRepository, times(1)).findByCode(any(String.class));
    }

}