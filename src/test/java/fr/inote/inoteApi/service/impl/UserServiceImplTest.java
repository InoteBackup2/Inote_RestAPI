package fr.inote.inoteApi.service.impl;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@Tag("Services_test")
public class UserServiceImplTest {

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

    @Mock
    private UserServiceImpl userServiceForInvoke;

    @InjectMocks
    private UserServiceImpl userService;

    private User userRef;
    private Validation validationRef;


    @BeforeEach
    void init() {
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        this.userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();
        this.userRepository.save(this.userRef);

        this.validationRef = Validation.builder()
                .code("123456")
                .user(this.userRef)
                .creation(Instant.now())
                .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
    }

    @DisplayName("Insert user in database with good parameters")
    @Test
    void createUser_shouldReturnUser_whenGoodParameters() throws NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Arrange
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

        when(this.userRepository.findByEmail(anotherUser.getEmail())).thenReturn(Optional.empty());
        when(this.passwordEncoder.encode(anotherUser.getPassword())).thenReturn("encodedPassword");
        when(this.roleRepository.findByName(any(RoleEnum.class))).thenReturn(Optional.of(roleForTest));
        when(this.userRepository.save(any(User.class))).thenReturn(anotherUser);

        // Act
        User userTest = (User) privateMethod_createUser.invoke(this.userService, anotherUser);

        // Assert
        assertThat(userTest).isNotNull();
        assertThat(userTest).isEqualTo(anotherUser);

        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));


    }

    @DisplayName("Insert user in database with Bad format email")
    @Test
    void createUser_shouldThrowException_whenBadEmailFormat() throws NoSuchMethodException,
            SecurityException, IllegalArgumentException {

        // Arrange

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

        // Act
        Throwable thrown = catchThrowable(() -> privateMethod_createUser.invoke(this.userService, anotherUser));

        // Assert
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(InoteInvalidEmailFormat.class);

    }

    @DisplayName("Insert user in database with Bad format password")
    @Test
    void createUser_shouldThrowException_whenEmailExistsInDatabase()
            throws NoSuchMethodException, SecurityException {
        // Arrange
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

        // Act
        Throwable thrown = catchThrowable(() -> privateMethod_createUser.invoke(this.userService, anotherUser));

        // Assert
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(InoteExistingEmailException.class);

        verify(this.userRepository, times(1)).findByEmail(any(String.class));
    }

    @DisplayName("Load an user registered in db with username")
    @Test
    public void loadUserByUsername_shouldReturnUser_whenUserNameIsPresent() {
        when(this.userRepository.findByEmail(this.userRef.getUsername())).thenReturn(Optional.of(this.userRef));

        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isNotNull();
        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isInstanceOf(User.class);
        assertThat(this.userService.loadUserByUsername(this.userRef.getUsername())).isEqualTo(this.userRef);

        verify(this.userRepository, times(3)).findByEmail(any(String.class));
    }


    @DisplayName("Load an user registered in db with bad username")
    @Test
    public void loadUserByUsername_shouldFail_whenUserNameIsNotPresent() {
        Optional<User> emptyOptional = Optional.empty();
        when(this.userRepository.findByEmail("OrichimaruDu93")).thenReturn(emptyOptional);

        Exception exception = assertThrows(UsernameNotFoundException.class, () -> this.userService.loadUserByUsername("OrichimaruDu93"));

        assertThat(exception.getMessage()).contains("None user found");

        verify(this.userRepository, times(1)).findByEmail(any(String.class));
    }

    @Test
    @DisplayName("Register an non-existing user with good parameter")
    void register_shouldSuccess_whenUserNotExistAndGoodParameters() throws InoteUserException, NoSuchMethodException, InoteExistingEmailException, InoteInvalidEmailFormat {
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

        assertThat(this.userService.register(anotherUser)).isEqualTo(anotherUser);

        verify(this.userRepository, times(1)).findByEmail(any(String.class));
        verify(this.passwordEncoder, times(1)).encode(any(String.class));
        verify(this.roleRepository, times(1)).findByName(any(RoleEnum.class));
        verify(this.userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Activate an user with good code")
    void activation_ShouldSuccess_whenCodeIsCorrect() throws InoteValidationNotFoundException, InoteUserNotFoundException, InoteValidationExpiredException {
        // Arrange
        when(this.validationService.getValidationFromCode(this.validationRef.getCode())).thenReturn(this.validationRef);
        when(this.validationRepository.save(any(Validation.class))).thenReturn(this.validationRef);
        when(this.userRepository.findById(any())).thenReturn(Optional.of(this.userRef));
        when(this.userRepository.save(any(User.class))).thenReturn(this.userRef);
        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");

        // Act
        User activatedUser = this.userService.activation(code);

        // Assert
        assertThat(activatedUser).isEqualTo(this.userRef);

        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));
        verify(this.validationRepository, times(1)).save(any(Validation.class));
//        verify(this.userRepository, times(1)).findById(any(Integer.class));
        verify(this.userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Activate an user with bad code")
    void activation_ShouldFail_whenCodeIsNotCorrect() throws InoteValidationNotFoundException {
        // Arrange
        when(this.validationService.getValidationFromCode(this.validationRef.getCode())).thenThrow(InoteValidationNotFoundException.class);
        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");

        // Act
        Throwable thrown = catchThrowable(() -> this.userService.activation(code));
        assertThat(thrown).isInstanceOf(InoteValidationNotFoundException.class);

        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));
    }

    @Test
    @DisplayName("Activate an user when user is not in database")
    void activation_ShouldFail_whenUserIsNotPresent() throws InoteValidationNotFoundException {
        // Arrange
        when(this.validationService.getValidationFromCode(this.validationRef.getCode())).thenReturn(this.validationRef);
        when(this.userRepository.findById(any())).thenReturn(Optional.empty());

        Map<String, String> code = new HashMap<>();
        code.put("code", "123456");

        // Act
        Throwable thrown = catchThrowable(() -> this.userService.activation(code));
        assertThat(thrown).isInstanceOf(InoteUserNotFoundException.class);

        verify(this.validationService, times(1)).getValidationFromCode(any(String.class));

    }
}
