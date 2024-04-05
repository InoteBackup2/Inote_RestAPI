package fr.inote.inoteApi.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
//@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@ExtendWith(MockitoExtension.class)
@Tag("Repositories_tests")
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    Role roleRef;

    @BeforeEach
    void init() {
//        this.roleRef = Role.builder().name(RoleEnum.ADMIN).build();
//        Role savedRole= this.roleRepository.save(this.roleRef);
//        assertThat(savedRole).isEqualTo(this.roleRef);
    }

    @DisplayName("Search existing role in database")
    @Test
    void findByName_shouldReturnOptionalOfRole_whenAskedRoleIsInDb() {
        Optional<Role> role = this.roleRepository.findByName(RoleEnum.ADMIN);
        assertThat(role).isNotEmpty();
        assertThat(role.get().getName()).isEqualTo(RoleEnum.ADMIN);
    }

    @DisplayName("Search non exists role in database")
    @Test
    void findByName_shouldReturnEmptyOptional_whenAskedRoleIsNotInDb() {
        Optional<Role> role = this.roleRepository.findByName(RoleEnum.MANAGER);
        assertThat(role).isEmpty();
    }
}
