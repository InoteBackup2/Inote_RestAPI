package fr.inote.inoteApi.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import org.springframework.test.context.ActiveProfiles;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DataJpaTest
//@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ExtendWith(MockitoExtension.class)
@Tag("Repositories_tests")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // Idealy, roleRepository should be mocked in integrality, but it has a 1-N
    // relationship with UserRepository;
    // So when saving an user with admin role for example, an instance
    // must be present in role table otherwise SGBDR generate Error.
    // So i think it not possible to mock the roleRepository, but the
    // save function is hibernate Implementaion, but not many risk.
    @Autowired
    private RoleRepository roleRepository;

    @Mock
    private RoleRepository mockedRoleRepository;

    User userRef;
    Role roleForTest;

    @BeforeEach
    void init() {
        // Just for the test. Roles of app are loaded at starting Application
        this.roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        this.roleRepository.save(this.roleForTest);

        when(this.mockedRoleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(this.roleForTest));

        this.userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(this.mockedRoleRepository.findByName(RoleEnum.ADMIN).orElseThrow())
                .build();
        assertThat(this.userRepository.save(this.userRef)).isEqualTo(this.userRef);

        verify(this.mockedRoleRepository, times(1)).findByName(any(RoleEnum.class));

    }

    @DisplayName("Search existing user in database")
    @Test
    void findByEmail_shouldReturnOptionalOfUser_WhenEmailIsCorrect() {
        Optional<User> optionalTestUser = this.userRepository.findByEmail(REFERENCE_USER_EMAIL);
        assertThat(optionalTestUser).isNotEmpty();
        User retrievedUser = optionalTestUser.get();
        assertThat(retrievedUser).isInstanceOf(User.class);
        assertThat(retrievedUser).isEqualTo(this.userRef);
    }

    @DisplayName("Search unknow user in database")
    @Test
    void findByEmail_shouldReturnNullOptionnal_WhenEmailIsNotCorrect() {
        Optional<User> testUser = this.userRepository.findByEmail("freezer@freezer21.uni");
        assertThat(testUser).isEmpty();
    }
}