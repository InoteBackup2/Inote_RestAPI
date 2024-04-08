package fr.inote.inoteApi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteExistingEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteInvalidEmailException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.AuthenticationDto;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Unit tests of AuthController layer
 *
 * @author atsuhiko Mochizuki
 * @date 04/04/2024
 */

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;


    @MockBean
    private JwtServiceImpl jwtServiceImpl;

    @MockBean
    private UserServiceImpl userService;

    private User userRef;


    @BeforeEach
    void init() {
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        this.userRef = User.builder()
                .email(REFERENCE_USER_NAME)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();
    }

    @Test
    @DisplayName("Register a non existing user")
    void register_ShouldSuccess_WithNotExistingUser() throws Exception {
        // Arrange
        when(this.userService.register(any(User.class))).thenReturn(this.userRef);

        UserDto userDto = new UserDto(
                this.userRef.getName(),
                this.userRef.getUsername(),
                this.userRef.getPassword());

        // Act
        ResultActions response = this.mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(userDto)));
        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers
                        .content()
                        .string(MessagesEn.REGISTER_OK_MAIL_SENDED));

        verify(this.userService, times(1)).register(any(User.class));
    }

    @Test
    @DisplayName("Attempt to register an existing user")
    void register_ShouldFail_WithExistingUser() throws Exception {
        // Arrange
        UserDto userDto = new UserDto(
                this.userRef.getName(),
                this.userRef.getUsername(),
                this.userRef.getPassword());
        when(this.userService.register(any(User.class)))
                .thenThrow(new InoteExistingEmailException());
        // doThrow(new
        // InoteExistingEmailException()).when(this.userService).register(any(User.class));

        // Act
        this.mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(userDto)))

                // Assert
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

        verify(this.userService, times(1)).register(any(User.class));
    }

    @Test
    @DisplayName("Activate an user with good code")
    void activation_ShouldSuccess_whenCodeIsCorrect() throws Exception {
        when(this.userService.activation(any(Map.class))).thenReturn(this.userRef);

        Map<String, String> bodyRequest = new HashMap<>();
        bodyRequest.put("code", "123456");

        // Act
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(MessagesEn.ACTIVATION_OF_USER_OK));
    }

    @Test
    @DisplayName("Activate an user with bad code")
    void activation_ShouldFail_whenCodeIsNotCorrect() throws Exception {
        when(this.userService.activation(any(Map.class))).thenThrow(InoteValidationNotFoundException.class);
        Map<String, String> bodyRequest = new HashMap<>();
        bodyRequest.put("code", "BadCode");

        // Act
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Sign in an existing user")
    void signIn_ShouldSuccess_WhenExistInSecurityContext() throws Exception {
        //Arrange
        AuthenticationDto userDtoTest = new AuthenticationDto(REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);
        Authentication mockInterface = Mockito.mock(Authentication.class, Mockito.CALLS_REAL_METHODS);
        when(this.authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockInterface);

        doAnswer(invocation -> {
            //String value = invocation.getArgument(0);
            mockInterface.setAuthenticated(true);
            return true;
        }).when(mockInterface).isAuthenticated();

        Map<String, String> mockResponse = new HashMap<>();
        mockResponse.put("bearer", REFERENCE_USER_BEARER);
        mockResponse.put("refresh", REFERENCE_USER_REFRESH_TOKEN);

        when(this.jwtServiceImpl.generate(REFERENCE_USER_EMAIL)).thenReturn(mockResponse);

        //Act
        ResultActions response = this.mockMvc.perform(post(Endpoint.SIGN_IN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(userDtoTest)));

        //Assert
        response.andExpect(MockMvcResultMatchers
                        .content()
                        .json(mockResponse.toString()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Sign in an non-existing user")
    void signIn_ShouldFail_WhenUserNotExistsInSecurityContext() throws Exception {
        //Arrange
        AuthenticationDto userDtoTest = new AuthenticationDto(REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);
        Authentication mockInterface = Mockito.mock(Authentication.class, Mockito.CALLS_REAL_METHODS);
        when(this.authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockInterface);

        doAnswer(invocation -> {
            mockInterface.setAuthenticated(false);
            return false;
        })
                .when(mockInterface)
                .isAuthenticated();

        //Act
        ResultActions response = this.mockMvc.perform(post(Endpoint.SIGN_IN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(userDtoTest)));

        //Assert
        response.andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("Change password of existing user")
    void changePassword_ShouldSuccess_WhenUsernameExists() throws Exception {

        //Arrange
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", REFERENCE_USER_EMAIL);

        doNothing().when(this.userService).changePassword(usernameMap);


        //Act
        ResultActions response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));

        //Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Attempt to change password of a non existing user")
    void changePassword_ShouldFail_WhenUsernameNotExist() throws Exception {
        //Arrange
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", "UnknowUser@neant.com");
        doThrow(UsernameNotFoundException.class).when(this.userService).changePassword(anyMap());

        ResultActions response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD
        )
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));

        //Assert
        response
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Attempt to change password with bad formated email")
    void changePassword_ShouldFail_WhenEmailIsBadFormated() throws Exception {
        //Arrange
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", "UnknowUser@@neant.com");
        doThrow(InoteInvalidEmailException.class).when(this.userService).changePassword(anyMap());

        ResultActions response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD
        )
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));

        //Assert
        response
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
