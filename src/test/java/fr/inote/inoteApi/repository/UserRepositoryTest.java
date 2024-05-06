package fr.inote.inoteApi.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import org.springframework.test.context.ActiveProfiles;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests of repository UserRepositoryTest
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
public class UserRepositoryTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* use @Mock create and inject mocked instances of classes */
    @Mock
    private RoleRepository mockedRoleRepository;

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * Use classical injection by constructor
     */
    private UserRepository userRepository;
    private RoleRepository roleRepository;

    // Constructor
    @Autowired
    public UserRepositoryTest(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void setUp() {}

    /* REPOSITORY UNIT TESTS */
    /* ============================================================ */

    // Idealy, roleRepository should be mocked in integrality, but it has a 1-N
    // relationship with UserRepository;
    // So when saving an user with admin role for example, an instance
    // must be present in role table otherwise SGBDR generate Error.
    // So i think it not possible to mock the roleRepository, but the
    // save function is hibernate Implementaion, but not many risk.

    User userRef;
    Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

    /* FIXTURES */
    /* ============================================================ */
    @BeforeEach
    void init() {
        this.roleRepository.save(this.roleForTest);
        when(this.mockedRoleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(this.roleForTest));
        this.userRef = User.builder()
            .email(REFERENCE_USER_EMAIL)
            .name(REFERENCE_USER_NAME)
            .password(REFERENCE_USER_PASSWORD)
            .role(this.mockedRoleRepository.findByName(RoleEnum.ADMIN).orElseThrow())
            .build();
        assertThat(this.userRepository.save(this.userRef)).isEqualTo(this.userRef);
    }

    /* REPOSITORY UNIT TESTS */
    /* ============================================================ */
    
    @Test
    @DisplayName("Search existing user in database")
    void findByEmail_shouldReturnOptionalOfUser_WhenEmailIsCorrect() {
        /*Act */
        Optional<User> optionalTestUser = this.userRepository.findByEmail(REFERENCE_USER_EMAIL);
        
        /*Assert */
        assertThat(optionalTestUser).isNotEmpty();
        User retrievedUser = optionalTestUser.get();
        assertThat(retrievedUser).isInstanceOf(User.class);
        assertThat(retrievedUser).isEqualTo(this.userRef);
    }

    @Test
    @DisplayName("Search unknow user in database")
    void findByEmail_shouldReturnNullOptionnal_WhenEmailIsNotCorrect() {
        /*Act */
        Optional<User> testUser = this.userRepository.findByEmail("freezer@freezer21.uni");
        
        /*Assert */
        assertThat(testUser).isEmpty();
    }
}