package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
@ActiveProfiles("test")
@DataJpaTest
//@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ExtendWith(MockitoExtension.class)
@Tag("Repositories_tests")
public class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    RefreshToken refreshTokenRef;

    @BeforeEach
    void init() {
        this.refreshTokenRef = RefreshToken.builder()
                .expirationStatus(false)
                .contentValue("kjfhqhfqfmrehfmoqehgiomhsmgkjdsfhgjkmdfskjghdsfhgms")
                .creationDate(Instant.now())
                .expirationDate(Instant.now().plus(10,ChronoUnit.MINUTES))
                .build();
    }

    @DisplayName("Save a refresh token in database")
    @Test
    void save_shouldReturnSuccessAndReturnRefreshToken() {
        RefreshToken returnValue = this.refreshTokenRepository.save(this.refreshTokenRef);
        assertThat(returnValue).isEqualTo(this.refreshTokenRef);
    }
}
