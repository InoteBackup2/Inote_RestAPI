package fr.inote.inoteApi.service.impl;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.ValidationRepository;
import fr.inote.inoteApi.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import fr.inote.inoteApi.service.UserService;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import org.springframework.test.context.ActiveProfiles;

/**
 * Unit tests of service UserService
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
public class UserServiceImplTest {

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* @Mock create and inject mocked instances of classes */
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ValidationRepository validationRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private ValidationService validationService;

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * @InjectMocks instance class to be tested and automatically inject mock fields
     * Nota : if service is an interface, instanciate implementation withs mocks in
     * params
     */
    @InjectMocks
    private UserService userService = new UserServiceImpl(userRepository, passwordEncoder, validationService,
            roleRepository, validationRepository);

    // For registerAdmin, not declared in interface
    @InjectMocks
    private UserServiceImpl userServiceImpl = new UserServiceImpl(userRepository, passwordEncoder, validationService,
            roleRepository, validationRepository);

    

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    private Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

    private User userRef = User.builder()
            .email(REFERENCE_USER_EMAIL)
            .name(REFERENCE_USER_NAME)
            .password(REFERENCE_USER_PASSWORD)
            .role(roleForTest)
            .build();

    private Validation validationRef = Validation.builder()
            .code("123456")
            .user(this.userRef)
            .creation(Instant.now())
            .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
            .build();

    /* FIXTURES */
    /* ============================================================ */
    @BeforeEach
    void init() {
        this.userRepository.save(this.userRef);
    }

    /* SERVICE UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Insert user in database with good parameters")
    void createUser_shouldReturnUser_whenGoodParameters() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        /* Arrange */
        // Access to the private method using reflection
        Method privateMethod_createUser = UserServiceImpl.class.getDeclaredMethod("createUser", User.class);
        privateMethod_createUser.setAccessible(true);

        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User anotherUser = User.builder()
                .email(REFERENCE_USER2_EMAIL)
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();

        when(this.userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(anotherUser.getPassword())).thenReturn("encodedPassword");
        when(this.roleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(roleForTest));
        when(this.userRepository.save(any(User.class))).thenReturn(anotherUser);

        /* Act */
        User userTest = (User) privateMethod_createUser.invoke(this.userService, anotherUser);

        /* Assert */
        assertThat(userTest).isNotNull();
        assertThat(userTest).isEqualTo(anotherUser);

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Insert user in database with Bad format email")
    void createUser_shouldThrowException_whenBadEmailFormat() throws NoSuchMethodException,
            SecurityException, IllegalArgumentException {

        /* Arrange */

        // Access to the private method using reflection
        Method privateMethod_createUser = UserServiceImpl.class.getDeclaredMethod("createUser", User.class);
        privateMethod_createUser.setAccessible(true);

        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User anotherUser = User.builder()
                .email("sangoku@@kame-house.uni")
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();

        /* Act */
        Throwable thrown = catchThrowable(() -> privateMethod_createUser.invoke(this.userService, anotherUser));

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(InoteInvalidEmailException.class);

    }

    @Test
    @DisplayName("Insert user in database with Bad format password")
    void createUser_shouldThrowException_whenEmailExistsInDatabase()
            throws NoSuchMethodException, SecurityException {
        /* Arrange */
        // Access to the private method using reflection
        Method privateMethod_createUser = UserServiceImpl.class.getDeclaredMethod("createUser", User.class);
        privateMethod_createUser.setAccessible(true);

        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User anotherUser = User.builder()
                .email(this.userRef.getEmail())
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();
        when(this.userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.of(this.userRef));

        /* Act */
        Throwable thrown = catchThrowable(() -> privateMethod_createUser.invoke(this.userService, anotherUser));

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(InoteExistingEmailException.class);

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
    }

    @Test
    @DisplayName("Create an non-existing admin user")
    void createTesterUser_shouldTesterUser_whenGoodParameters() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        /* Arrange */
        // Access to the private method using reflection
        Method privateMethod_createTesterUser = UserServiceImpl.class.getDeclaredMethod("createTesterUser", User.class);
        privateMethod_createTesterUser.setAccessible(true);

        Role roleForTest = Role.builder().name(RoleEnum.TESTER).build();
        User anotherUser = User.builder()
                .email(REFERENCE_USER2_EMAIL)
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();

        when(this.userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(anotherUser.getPassword())).thenReturn("encodedPassword");
        when(this.roleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(roleForTest));
        when(this.userRepository.save(any(User.class))).thenReturn(anotherUser);

        /* Act */
        User userTest = (User) privateMethod_createTesterUser.invoke(this.userService, anotherUser);

        /* Assert */
        assertThat(userTest).isNotNull();
        assertThat(userTest).isEqualTo(anotherUser);

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
    }

    @Test
    @DisplayName("Load an user registered in db with username")
    public void loadUserByUsername_shouldReturnUser_whenUserNameIsPresent() {
        /* Arrange */
        when(this.userRepository.findByEmail(this.userRef.getUsername())).thenReturn(Optional.of(this.userRef));

        /* Act & assert */
        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isNotNull();
        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isInstanceOf(User.class);
        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isEqualTo(this.userRef);

        /* Verify */
        verify(this.userRepository, times(3)).findByEmail(any(String.class));
    }

    @Test
    @DisplayName("Load an user registered in db with bad username")
    public void loadUserByUsername_shouldFail_whenUserNameIsNotPresent() {
        /* Arrange */
        when(this.userRepository.findByEmail("OrichimaruDu93")).thenReturn(Optional.empty());

        /* Act & assert */
        Exception exception = assertThrows(UsernameNotFoundException.class,
                () -> this.userService.loadUserByUsername("OrichimaruDu93"));

        assertThat(exception.getMessage()).contains("None user found");

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
    }

    @Test
    @DisplayName("Register an non-existing user with good parameter")
    void register_shouldSuccess_whenUserNotExistAndGoodParameters()
            throws NoSuchMethodException, InoteExistingEmailException, InoteInvalidEmailException,
            InoteRoleNotFoundException, InoteInvalidPasswordFormatException, MailException, InoteMailException {

        /* Arrange */
        Method privateMethod_createUser = UserServiceImpl.class.getDeclaredMethod("createUser", User.class);
        privateMethod_createUser.setAccessible(true);

        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User anotherUser = User.builder()
                .email(REFERENCE_USER2_EMAIL)
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();

        when(this.userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(anotherUser.getPassword())).thenReturn("encodedPassword");
        when(this.roleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(roleForTest));
        when(this.userRepository.save(any(User.class))).thenReturn(anotherUser);

        /* Act & assert */
        assertThat(this.userService.register(anotherUser)).isEqualTo(anotherUser);

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
        verify(this.userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Register an non-existing admin user with good parameter")
    void registerAdmin_shouldSuccess_whenUserNotExistAndGoodParameters()
            throws NoSuchMethodException, InoteExistingEmailException, InoteInvalidEmailException,
            InoteRoleNotFoundException, InoteInvalidPasswordFormatException, MailException, InoteMailException {

        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User anotherUser = User.builder()
                .email(REFERENCE_USER2_EMAIL)
                .name(REFERENCE_USER2_NAME)
                .password(REFERENCE_USER2_PASSWORD)
                .role(roleForTest)
                .build();

        when(this.userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(anotherUser.getPassword())).thenReturn("encodedPassword");
        when(this.roleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(roleForTest));
        when(this.userRepository.save(any(User.class))).thenReturn(anotherUser);

        /* Act & assert */
        assertThat(this.userServiceImpl.registerTester(anotherUser).getPassword()).isEqualTo(anotherUser.getPassword());

        /* Verify */
        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
        verify(this.userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Activate an user with good code")
    void activation_ShouldSuccess_whenCodeIsCorrect()
            throws InoteValidationNotFoundException, InoteUserNotFoundException, InoteValidationExpiredException {

        /* Arrange */
        when(this.validationService.getValidationFromCode(this.validationRef.getCode())).thenReturn(this.validationRef);
        when(this.validationRepository.save(any(Validation.class))).thenReturn(this.validationRef);
        when(this.userRepository.findById(any())).thenReturn(Optional.of(this.userRef));
        when(this.userRepository.save(any(User.class))).thenReturn(this.userRef);

        /* Act */
        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");
        User activatedUser = this.userService.activation(code);

        /* Assert */
        assertThat(activatedUser).isEqualTo(this.userRef);

        /* Verify */
        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));
        verify(this.validationRepository, times(1)).save(any(Validation.class));
        verify(this.userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Activate an user with bad code")
    void activation_ShouldFail_whenCodeIsNotCorrect() throws InoteValidationNotFoundException {

        /* Arrange */
        when(this.validationService.getValidationFromCode(this.validationRef.getCode()))
                .thenThrow(InoteValidationNotFoundException.class);

        /* Act */
        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");
        Throwable thrown = catchThrowable(() -> this.userService.activation(code));

        /* Assert */
        assertThat(thrown).isInstanceOf(InoteValidationNotFoundException.class);

        /* Verify */
        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));
    }

    @Test
    @DisplayName("Activate an user when user is not in database")
    void activation_ShouldFail_whenUserIsNotPresent() throws InoteValidationNotFoundException {
        /* Arrange */
        when(this.validationService.getValidationFromCode(this.validationRef.getCode())).thenReturn(this.validationRef);
        when(this.userRepository.findById(any())).thenReturn(Optional.empty());

        /* Act */
        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");
        Throwable thrown = catchThrowable(() -> this.userService.activation(code));
        assertThat(thrown).isInstanceOf(InoteUserNotFoundException.class);

        /* Verify */
        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));
    }

    @Test
    @DisplayName("Check password with security requirements satisfied")
    void checkPasswordSecurityRequirements_ShouldSuccess_WhenSecurityRequirementsSatisfied()
            throws NoSuchMethodException {

        /* Arrange */
        Method privateMethod_checkPasswordSecurityRequirements = UserServiceImpl.class
                .getDeclaredMethod("checkPasswordSecurityRequirements", String.class);
        privateMethod_checkPasswordSecurityRequirements.setAccessible(true);

        /* Act & assert */
        assertThatCode(() -> {
            this.userService.checkPasswordSecurityRequirements("aA1$shkfkh_A86s36erff3s3w8ez6?!");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Check password with security requirements unsatisfied")
    void checkPasswordSecurityRequirements_ShouldFail_WhenSecurityRequirementsAreNotSatisfied()
            throws NoSuchMethodException {

        /* Act & assert */
        // No uppercase letter
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements("12345678$a");
        });

        // No lowercase letter
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements("12345678$A");
        });

        // No special character
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements("12345678Aa");
        });

        // No digit
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements("??????????Aa");
        });

        // no minimum number caracters
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements("1Aa$");
        });

        // too many caracters
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.checkPasswordSecurityRequirements(
                    "1Aa$113??adrssdhfhdskfjksdhjkfhdsjkhfjkdshkjfhdsjkhfksdhkfhdskhfkdshkjfhskjhfkjshkdfhdsjkhfksdhfhsdjkhfjkdshkf");
        });
    }

    @Test
    @DisplayName("set a new password to an user")
    void newPassword_ShouldSuccess_WhenUserAndValidationExistsAndNewPasswordIsEnoughSecure()
            throws InoteInvalidPasswordFormatException, InoteValidationNotFoundException {

        /* Arrange */
        when(this.validationService.getValidationFromCode(any(String.class))).thenReturn(this.validationRef);
        when(this.passwordEncoder.encode(any(String.class))).thenReturn("As789?!fsfsfsfsfsfs");
        when(this.userRepository.save(any(User.class))).thenReturn(this.userRef);
        when(this.userRepository.findByEmail(any(String.class))).thenReturn(Optional.of(this.userRef));

        /* Act & assert */
        assertThatCode(() -> {
            this.userService.newPassword(
                    this.userRef.getEmail(),
                    this.userRef.getPassword(),
                    "123465");
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("set new password user when user is not referenced in db")
    void newPassword_ShouldFail_WhenUserNotExists() {

        /* Act & assert */
        assertThatExceptionOfType(UsernameNotFoundException.class).isThrownBy(() -> {
            this.userService.newPassword(
                    "unknow@gmail.com",
                    this.userRef.getPassword(),
                    "123465");
        });
    }

    @Test
    @DisplayName("set new password when code not corresponding to existing validation")
    void newPassword_ShouldFail_WhenCodeNotCorrespondingToAnyValidation() throws InoteValidationNotFoundException {

        /* Arrange */
        when(this.userRepository.findByEmail(any(String.class))).thenReturn(Optional.of(this.userRef));
        when(this.validationService.getValidationFromCode(any(String.class)))
                .thenThrow(InoteValidationNotFoundException.class);

        /* Act & assert */
        assertThatExceptionOfType(InoteValidationNotFoundException.class).isThrownBy(() -> {
            this.userService.newPassword(
                    this.userRef.getEmail(),
                    this.userRef.getPassword(),
                    "7798798794664646565464645646546465464");
        });
    }

    @Test
    @DisplayName("set new password when desired new password is not enough secured")
    void newPassword_ShouldFail_WhenPasswordIsNotEnoughSecured()
            throws InoteInvalidPasswordFormatException, InoteValidationNotFoundException {

        /* Arrange */
        when(this.userRepository.findByEmail(any(String.class))).thenReturn(Optional.of(this.userRef));
        when(this.validationService.getValidationFromCode(any(String.class))).thenReturn(this.validationRef);

        /* Act & assert */
        assertThatExceptionOfType(InoteInvalidPasswordFormatException.class).isThrownBy(() -> {
            this.userService.newPassword(
                    this.userRef.getEmail(),
                    "1234",
                    this.validationRef.getCode());
        });
    }

}
