package fr.inote.inoteApi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.*;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Unit tests of Controller layer AuthController
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */
/*
 * Dedicated for unit test mvc controllers:
 * -disable full-auto configuration
 * -apply config only relevant mvc tests
 * -autoconfigure mockMvc instance (used to test the dispatcher servlet and your
 * controllers)
 */
@WebMvcTest(AuthController.class)

/*
 * Enables all autoconfiguration related to MockMvc and ONLY MockMvc + none
 * Spring security filters applied
 */
@AutoConfigureMockMvc(addFilters = false)

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

        /* DEPENDENCIES INJECTION */
        /* ============================================================ */

        /*
         * MockMvc provides a convenient way to send requests to your application and
         * inspect the
         * responses, allowing you to verify the behavior of your controllers in
         * isolation.
         * -> Need to be autowired to be autoconfigured
         */
        @Autowired
        private MockMvc mockMvc;

        /* ObjectMapper provide functionalities for read and write JSON data's */
        @Autowired
        private ObjectMapper objectMapper;

        /* DEPENDENCIES MOCKING */
        /* ============================================================ */
        @MockBean
        private AuthenticationManager authenticationManager;
        @MockBean
        private JwtServiceImpl jwtServiceImpl;
        @MockBean
        private UserServiceImpl userService;

        /* REFERENCES FOR MOCKING */
        /* ============================================================ */
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        private User userRef = User.builder()
                        .email(REFERENCE_USER_NAME)
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

        private RefreshToken refreshTokenRef = RefreshToken.builder()
                        .expirationStatus(false)
                        .contentValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiU2FuZ29rdSIsImV4cCI6MTg2OTY3NTk5Niwic3ViIjoic2FuZ29rdUBpbm90ZS5mciJ9.ni8Z4Wiyo6-noIme2ydnP1vHl6joC0NkfQ-lxF501vY")
                        .creationDate(Instant.now())
                        .expirationDate(Instant.now().plus(10, ChronoUnit.MINUTES))
                        .build();

        private Jwt jwtRef = Jwt.builder()
                        .id(1)
                        .user(this.userRef)
                        .contentValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                        .deactivated(false)
                        .expired(false)
                        .refreshToken(this.refreshTokenRef)
                        .build();

        private UserDto userDtoRef = new UserDto(
                        this.userRef.getName(),
                        this.userRef.getUsername(),
                        this.userRef.getPassword());

        /* FIXTURES */
        /* ============================================================ */
        // @BeforeEach
        // void init() {}

        /* CONTROLLER UNIT TEST */
        /* ============================================================ */
        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Register a non existing user")
        void register_ShouldSuccess_WithNotExistingUser() throws Exception {
                /* Arrange */
                when(this.userService.register(any(User.class))).thenReturn(this.userRef);

                /* Act & assert */
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> map = mapper.readValue(returnedResponse, Map.class);
                assertThat(map.size()).isEqualTo(1);
                assertThat(map.get("msg")).isEqualTo(MessagesEn.ACTIVATION_NEED_ACTIVATION);

                /* Mocking invocation check */
                verify(this.userService, times(1)).register(any(User.class));
        }

        @Test
        @DisplayName("Attempt to register an existing user")
        void register_ShouldFail_WithExistingUser() throws Exception {
                /* Arrange */
                when(this.userService.register(any(User.class)))
                                .thenThrow(new InoteExistingEmailException());

                /* Act & assert */
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
                /* Mocking invocation check */
                verify(this.userService, times(1)).register(any(User.class));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Activate an user with good code")
        void activation_ShouldSuccess_whenCodeIsCorrect() throws Exception {

                /* Arrange */
                when(this.userService.activation(anyMap())).thenReturn(this.userRef);

                /* Act & assert */
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", "123456");
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content()
                                                .contentType(MediaType.APPLICATION_JSON_VALUE));

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> map = mapper.readValue(returnedResponse, Map.class);
                assertThat(map.size()).isEqualTo(1);
                assertThat(map.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                /* Mocking invocation check */
                verify(this.userService, times(1)).activation(anyMap());
        }

        @Test
        @DisplayName("Activate an user with bad code")
        void activation_ShouldFail_whenCodeIsNotCorrect() throws Exception {
                /* Arrange */
                when(this.userService.activation(anyMap())).thenThrow(InoteValidationNotFoundException.class);

                /* Act & assert */
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", "BadCode");
                this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Sign in an existing user")
        void signIn_ShouldSuccess_WhenExistInSecurityContext() throws Exception {

                /* Arrange */
                AuthenticationDto userDtoTest = new AuthenticationDto(REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);
                Authentication mockInterface = Mockito.mock(Authentication.class, Mockito.CALLS_REAL_METHODS);
                when(this.authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(mockInterface);

                doAnswer(invocation -> {
                        // String value = invocation.getArgument(0);
                        mockInterface.setAuthenticated(true);
                        return true;
                }).when(mockInterface).isAuthenticated();

                Map<String, String> mockResponse = new HashMap<>();
                mockResponse.put("bearer", REFERENCE_USER_BEARER);
                mockResponse.put("refresh", REFERENCE_USER_REFRESH_TOKEN);

                when(this.jwtServiceImpl.generate(REFERENCE_USER_EMAIL)).thenReturn(mockResponse);

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_IN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(userDtoTest)))
                                .andExpect(MockMvcResultMatchers
                                                .content()
                                                .json(mockResponse.toString()))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Sign in an non-existing user")
        void signIn_ShouldFail_WhenUserNotExistsInSecurityContext() throws Exception {

                /* Arrange */
                AuthenticationDto userDtoTest = new AuthenticationDto(REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);
                Authentication mockInterface = Mockito.mock(Authentication.class, Mockito.CALLS_REAL_METHODS);
                when(this.authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                                .thenReturn(mockInterface);

                doAnswer(invocation -> {
                        mockInterface.setAuthenticated(false);
                        return false;
                })
                                .when(mockInterface)
                                .isAuthenticated();

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_IN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(userDtoTest)))
                                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Change password of existing user")
        void changePassword_ShouldSuccess_WhenUsernameExists() throws Exception {
                /* Arrange */
                doNothing().when(this.userService).changePassword(anyMap());

                /* Act & assert */
                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", REFERENCE_USER_EMAIL);
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Attempt to change password of a non existing user")
        void changePassword_ShouldFail_WhenUsernameNotExist() throws Exception {
                /* Arrange */
                doThrow(UsernameNotFoundException.class).when(this.userService).changePassword(anyMap());

                /* Act & assert */
                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", "UnknowUser@neant.com");
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Attempt to change password with bad formated email")
        void changePassword_ShouldFail_WhenEmailIsBadFormated() throws Exception {
                /* Arrange */
                doThrow(InoteInvalidEmailException.class).when(this.userService).changePassword(anyMap());

                /* Act & assert */
                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", "UnknowUser@@neant.com");

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password of existing user")
        void newPassword_ShouldSuccess_WhenUserExists() throws Exception {
                /* Arrange */
                doNothing().when(this.userService).newPassword(anyString(), anyString(), anyString());

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.validationRef.getUser().getEmail(),
                                this.validationRef.getCode(),
                                this.validationRef.getUser().getPassword());

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content().string(MessagesEn.NEW_PASSWORD_SUCCESS));

        }

        @Test
        @DisplayName("set new password of non existing user")
        void newPassword_ShouldFail_WhenUserNotExists() throws Exception {
                /* Arrange */
                doThrow(UsernameNotFoundException.class).when(this.userService).newPassword(anyString(),
                                anyString(), anyString());

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.validationRef.getUser().getEmail(),
                                this.validationRef.getCode(),
                                this.validationRef.getUser().getPassword());

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password with non-referenced validation by code")
        void newPassword_ShouldFail_WhenValidationNotExists() throws Exception {
                /* Arrange */
                doThrow(InoteValidationNotFoundException.class).when(this.userService).newPassword(anyString(),
                                anyString(), anyString());

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.validationRef.getUser().getEmail(),
                                "0000000000000000000",
                                this.validationRef.getUser().getPassword());

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password with password not enough secured")
        void newPassword_ShouldFail_WhenPasswordNotEnoughSecured() throws Exception {

                /* Arrange */
                doThrow(InoteInvalidPasswordFormatException.class).when(this.userService).newPassword(anyString(),
                                anyString(), anyString());

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.validationRef.getUser().getEmail(),
                                this.validationRef.getCode(),
                                "1234");
                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Refresh connection with correct refresh token value")
        void refreshConnectionWithRefreshTokenValue_ShouldSuccess_WhenRefreshTokenValueIsCorrect() throws Exception {
                /* Arrange */
                Map<String, String> refreshToken = new HashMap<>();
                refreshToken.put(REFRESH, this.jwtRef.getRefreshToken().getContentValue());
                when(this.jwtServiceImpl.refreshConnectionWithRefreshTokenValue(anyString()))
                                .thenReturn(refreshToken);

                /* Act & assert */
                RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto(this.jwtRef
                                .getRefreshToken()
                                .getContentValue());

                this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
                                .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        @Test
        @DisplayName("Refresh connection with bad refresh token value")
        void refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenValueIsBad() throws Exception {

                /* Arrange */
                when(this.jwtServiceImpl.refreshConnectionWithRefreshTokenValue(anyString()))
                                .thenThrow(InoteJwtNotFoundException.class);

                /* Act & assert */
                RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto("bad_refresh_token_value");

                this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Refresh connection with expired refresh token")
        void refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenIsExpired() throws Exception {

                /* Arrange */
                when(this.jwtServiceImpl.refreshConnectionWithRefreshTokenValue(anyString()))
                                .thenThrow(InoteExpiredRefreshTokenException.class);

                /* Act & assert */
                RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto("bad_refresh_token_value");

                this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Signout a connected user")
        void signOut_ShouldSuccess_WhenUserIsConnected() throws Exception {
                /* Arrange */
                doNothing().when(this.jwtServiceImpl).signOut();

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_OUT))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("SignOut an non existing user")
        void signout_ShouldFail_whenJwtOfPrincipalUserIsNotFound() throws Exception {

                /* Arrange */
                doThrow(InoteJwtNotFoundException.class).when(this.jwtServiceImpl).signOut();

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_OUT))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

}
