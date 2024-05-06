package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests of repository RefreshTokenRepositoryTest
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
public class RefreshTokenRepositoryTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* use @Mock create and inject mocked instances of classes */

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * Use classical injection by constructor
     */
    private RefreshTokenRepository refreshTokenRepository;

    // Constructor
    @Autowired
    public RefreshTokenRepositoryTest(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    RefreshToken refreshTokenRef = RefreshToken.builder()
            .expirationStatus(false)
            .contentValue("kjfhqhfqfmrehfmoqehgiomhsmgkjdsfhgjkmdfskjghdsfhgms")
            .creationDate(Instant.now())
            .expirationDate(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void init() {}

    /* REPOSITORY UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Save a refresh token in database")
    void save_shouldReturnSuccessAndReturnRefreshToken() {
        /*Act */
        RefreshToken returnValue = this.refreshTokenRepository.save(this.refreshTokenRef);
        
        /*Assert */
        assertThat(returnValue).isEqualTo(this.refreshTokenRef);
    }
}
