package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests of repository ValidationRepositoryTest
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * @DataJpaTest is an annotation in Spring Boot that is used to test JPA
 * repositories.
 * It focuses only on JPA components and disables full auto-configuration,
 * applying
 * only the configuration relevant to JPA tests.
 */
@DataJpaTest
/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes.
 * Nota : here used for using another database ok main app
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
class ValidationRepositoryTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* use @Mock create and inject mocked instances of classes */
    // Ideally, roleRepository should be mocked in integrality, but it has a 1-N
    // relationship with UserRepository;
    // So when saving an user with admin role for example, an instance
    // must be present in role table otherwise SGBDR generate Error.
    // So i think it not possible to mock the roleRepository, but the
    // save function is hibernate Implementation, but not many risk.

    @Mock
    private RoleRepository mockedRoleRepository;

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * Use classical injection by constructor
     */
    private ValidationRepository validationRepository;
    private RoleRepository roleRepository;
    private UserRepository userRepository;

    // Constructor
    @Autowired
    public ValidationRepositoryTest(
            RoleRepository mockedRoleRepository,
            RoleRepository roleRepository,
            ValidationRepository validationRepository,
            UserRepository userRepository) {
        this.mockedRoleRepository = mockedRoleRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.validationRepository = validationRepository;
    }

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    Role roleForTest;
    User userRef;
    Validation validationRef;

    /* FIXTURES */
    /* ============================================================ */
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

    /* REPOSITORY UNIT TESTS */
    /* ============================================================ */

    @Test
    @DisplayName("Search a Validation in database with correct code")
    void findByCode_shouldReturnOptionalOfValidation_whenCodeIsCorrect() {
        /* Act */
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());

        /* Assert */
        assertThat(optionalOfValidation).isNotEmpty();
        Validation foundValidation = optionalOfValidation.get();
        assertThat(foundValidation.getActivation()).isEqualTo(this.validationRef.getActivation());
        assertThat(foundValidation.getId()).isEqualTo(this.validationRef.getId());
        assertThat(foundValidation.getExpiration()).isEqualTo(this.validationRef.getExpiration());
        assertThat(foundValidation.getCreation()).isEqualTo(this.validationRef.getCreation());
        assertThat(foundValidation.getUser()).isEqualTo(this.validationRef.getUser());
        assertThat(foundValidation.getCode()).isEqualTo(this.validationRef.getCode());
    }

    @Test
    @DisplayName("Search a Validation in database with incorrect code")
    void findByCode_shouldReturnOptionalOfValidation_whenCodeIsNotCorrect() {
        /* Act */
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode("INCORRECT_CODE");

        /* Assert */
        assertThat(optionalOfValidation).isEmpty();
    }

    @Test
    @DisplayName("Delete all validations when expired")
    void deleteAllByExpirationBefore_shouldDeleteExpiredValidation_whenInstantIsAfter() {

        /* Act & assert */
        Optional<Validation> optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isNotEmpty();
        Validation foundValidation = optionalOfValidation.get();
        assertThat(foundValidation.getActivation()).isEqualTo(this.validationRef.getActivation());
        Instant expirationTime = foundValidation.getExpiration();

        this.validationRepository.deleteAllByExpirationBefore(expirationTime.plus(10, ChronoUnit.MINUTES));

        optionalOfValidation = this.validationRepository.findByCode(this.validationRef.getCode());
        assertThat(optionalOfValidation).isEmpty();
    }

    @Test
    @DisplayName("Attempt to delete a validation not expired")
    void deleteAllByExpirationBefore_shouldNotDeleteValidation_whenInstantIsBefore() {
        
        /* Act & assert */
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