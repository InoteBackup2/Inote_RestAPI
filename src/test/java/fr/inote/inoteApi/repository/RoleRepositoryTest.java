package fr.inote.inoteApi.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests of repository
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
public class RoleRepositoryTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* use @Mock create and inject mocked instances of classes */

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * Use classical injection by constructor
     */
    private RoleRepository roleRepository;

    // Constructor
    @Autowired
    public RoleRepositoryTest(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    Role roleRef;

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void init() {}

    /* REPOSITORY UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Search existing role in database")
    void findByName_shouldReturnOptionalOfRole_whenAskedRoleIsInDb() {
        
        /* Act */
        Optional<Role> role = this.roleRepository.findByName(RoleEnum.ADMIN);

        /* Assert */
        assertThat(role).isNotEmpty();
        assertThat(role.get().getName()).isEqualTo(RoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Search non exists role in database")
    void findByName_shouldReturnEmptyOptional_whenAskedRoleIsNotInDb() {
        
        /* Act */
        Optional<Role> role = this.roleRepository.findByName(null);

        /* Assert */
        assertThat(role).isEmpty();
    }
}
