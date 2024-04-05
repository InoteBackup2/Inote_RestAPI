package fr.inote.inoteApi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteExistingEmailException;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;

import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;

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
}
