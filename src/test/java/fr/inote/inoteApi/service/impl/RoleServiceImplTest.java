package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.repository.RoleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * The type Role service impl test.
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@Tag("Services_test")
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    /* References for test*/
    List<Role> rolesRef = null;
    Role admin, manager, user;

    @BeforeEach
    void setUp() {
        this.admin = Role.builder().name(RoleEnum.ADMIN).build();
        this.manager = Role.builder().name(RoleEnum.MANAGER).build();
        this.user = Role.builder().name(RoleEnum.USER).build();

        /* Load existing roles in App */
        this.rolesRef = new ArrayList<>();
        for (RoleEnum item : RoleEnum.values()) {
            this.rolesRef.add(Role.builder().name(item).build());
        }
    }

    @Test
    @DisplayName("Insert the application roles in db")
    void insertRolesInDb_shouldReturnAnArrayWithNumberOfRoleApp() {
        when(this.roleRepository.save(ArgumentMatchers.any(Role.class))).thenReturn(ArgumentMatchers.any(Role.class));

        List<Role> roles;
        roles = this.roleService.insertRolesInDb();
        assertThat(roles).isNotNull();
        assertThat(roles.size()).isEqualTo(this.rolesRef.size());

        verify(this.roleRepository, times(RoleEnum.values().length)).save(any(Role.class));
    }

    @Test
    @DisplayName("Load admin role from database")
    void loadAdminRole_shouldSuccess() throws InoteUserException {
        when(this.roleRepository.findByName(RoleEnum.ADMIN)).thenReturn(Optional.of(this.admin));

        Role role;
        role = this.roleService.loadAdminRole();
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.ADMIN);

        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Load manager role from database")
    void loadManagerRole_shouldSuccess() throws InoteUserException {
        when(this.roleRepository.findByName(RoleEnum.MANAGER)).thenReturn(Optional.of(this.manager));

        Role role;
        role = this.roleService.loadManagerRole();
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.MANAGER);

        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Load user role from database")
    void loadUserRole_shouldSuccess() throws InoteUserException {
        when(this.roleRepository.findByName(RoleEnum.USER)).thenReturn(Optional.of(this.user));

        Role role;
        role = this.roleService.loadUserRole();
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.USER);

        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }
}