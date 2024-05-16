package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.service.RoleService;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests of service RoleService
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* @Mock create and inject mocked instances of classes */
    @Mock
    private RoleRepository roleRepository;

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * @InjectMocks instance class to be tested and automatically inject mock fields
     * Nota : if service is an interface, instanciate implementation withs mocks in
     * params
     */
    @InjectMocks
    private RoleService roleService = new RoleServiceImpl(roleRepository);

    

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    List<Role> rolesRef = null;
    private Role admin = Role.builder().name(RoleEnum.ADMIN).build();
    private Role manager = Role.builder().name(RoleEnum.MANAGER).build();
    private Role user = Role.builder().name(RoleEnum.USER).build();

    /* FIXTURES */
    /* ============================================================ */
    @BeforeEach
    void setUp() {
        /* Load existing roles in App */
        this.rolesRef = new ArrayList<>();
        for (RoleEnum item : RoleEnum.values()) {
            this.rolesRef.add(Role.builder().name(item).build());
        }
    }

    /* SERVICE UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Insert the application roles in db")
    void insertRolesInDb_shouldReturnAnArrayWithNumberOfRoleApp() {

        /* Arrange */
        when(this.roleRepository.save(ArgumentMatchers.any(Role.class))).thenReturn(ArgumentMatchers.any(Role.class));

        /* Act */
        List<Role> roles = this.roleService.insertRolesInDb();

        /* Assert */
        assertThat(roles).isNotNull();
        assertThat(roles.size()).isEqualTo(this.rolesRef.size());

        /* Verify */
        verify(this.roleRepository, times(RoleEnum.values().length)).save(any(Role.class));
    }

    @Test
    @DisplayName("Load admin role from database")
    void loadAdminRole_shouldSuccess() throws InoteUserException {

        /* Arrange */
        when(this.roleRepository.findByName(RoleEnum.ADMIN)).thenReturn(Optional.of(this.admin));

        /* Act */
        Role role = this.roleService.loadAdminRole();

        /* Assert */
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.ADMIN);

        /* Verify */
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Load manager role from database")
    void loadManagerRole_shouldSuccess() throws InoteUserException {

        /* Arrange */
        when(this.roleRepository.findByName(RoleEnum.MANAGER)).thenReturn(Optional.of(this.manager));

        /* Act */
        Role role = this.roleService.loadManagerRole();

        /* Assert */
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.MANAGER);

        /* Verify */
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Load user role from database")
    void loadUserRole_shouldSuccess() throws InoteUserException {

        /* Arrange */
        when(this.roleRepository.findByName(RoleEnum.USER)).thenReturn(Optional.of(this.user));

        /* Act */
        Role role = this.roleService.loadUserRole();

        /* Assert */
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo(RoleEnum.USER);

        /* Verify */
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }
}