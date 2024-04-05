package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * The type Validation repository test.
 *
 * @author Atsuhiko Mochizuki
 */
@ActiveProfiles("test")
@DataJpaTest
//@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ExtendWith(MockitoExtension.class)
@Tag("Repositories_tests")
class ValidationRepositoryTest {

    @Autowired
    private ValidationRepository validationRepository;

    // Ideally, roleRepository should be mocked in integrality, but it has a 1-N
    // relationship with UserRepository;
    // So when saving an user with admin role for example, an instance
    // must be present in role table otherwise SGBDR generate Error.
    // So i think it not possible to mock the roleRepository, but the
    // save function is hibernate Implementation, but not many risk.

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    @Mock
    private RoleRepository mockedRoleRepository;
    Role roleForTest;
    User userRef;
    Validation validationRef;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {

        // Insert a validation in database
        this.roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        assertThat(this.roleRepository.save(this.roleForTest)).isEqualTo(this.roleForTest);

        when(this.mockedRoleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(this.roleForTest));
        this.userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(this.mockedRoleRepository.findByName(RoleEnum.ADMIN).orElseThrow())
                .build();
        assertThat(this.userRepository.save(this.userRef)).isEqualTo(this.userRef);
        verify(this.mockedRoleRepository, times(1)).findByName(any(RoleEnum.class));

        Instant instantCreation = Instant.now();
        this.validationRef = Validation.builder()
                .code("1234")
                .user(userRef)
                .creation(instantCreation)
                .expiration(instantCreation.plus(5, ChronoUnit.MINUTES))
                .activation(instantCreation.plus(2, ChronoUnit.MINUTES))
                .code(String.format("%06d", new Random().nextInt(999999)))
                .build();
        assertThat(this.validationRepository.save(this.validationRef)).isEqualTo(this.validationRef);

    }

    @DisplayName("Search a Validation in database with correct code")
    @Test
    void findByCode_shouldReturnOptionalOfValidation_whenCodeIsCorrect() {
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isNotEmpty();
        Validation foundValidation = optionalOfValidation.get();
        assertThat(foundValidation.getActivation()).isEqualTo(this.validationRef.getActivation());
        assertThat(foundValidation.getId()).isEqualTo(this.validationRef.getId());
        assertThat(foundValidation.getExpiration()).isEqualTo(this.validationRef.getExpiration());
        assertThat(foundValidation.getCreation()).isEqualTo(this.validationRef.getCreation());
        assertThat(foundValidation.getUser()).isEqualTo(this.validationRef.getUser());
        assertThat(foundValidation.getCode()).isEqualTo(this.validationRef.getCode());
    }

    @DisplayName("Search a Validation in database with incorrect code")
    @Test
    void findByCode_shouldReturnOptionalOfValidation_whenCodeIsNotCorrect() {
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode("INCORRECT_CODE");
        assertThat(optionalOfValidation).isEmpty();
    }


    @DisplayName("Delete all validations when expired")
    @Test
    void deleteAllByExpirationBefore_shouldDeleteExpiredValidation_whenInstantIsAfter() {
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isNotEmpty();
        Validation foundValidation = optionalOfValidation.get();
        assertThat(foundValidation.getActivation()).isEqualTo(this.validationRef.getActivation());
        Instant expirationTime = foundValidation.getExpiration();

        this.validationRepository.deleteAllByExpirationBefore(expirationTime.plus(10, ChronoUnit.MINUTES));

        optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isEmpty();
    }

    @DisplayName("Attempt to delete a validation not expired")
    @Test
    void deleteAllByExpirationBefore_shouldNotDeleteValidation_whenInstantIsBefore() {
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isNotEmpty();
        Validation foundValidation = optionalOfValidation.get();
        assertThat(foundValidation.getActivation()).isEqualTo(this.validationRef.getActivation());
        Instant expirationTime = foundValidation.getExpiration();

        this.validationRepository.deleteAllByExpirationBefore(expirationTime.minus(1, ChronoUnit.MINUTES));

        optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isNotEmpty();
        assertThat(optionalOfValidation.get()).isEqualTo(this.validationRef);
    }
}