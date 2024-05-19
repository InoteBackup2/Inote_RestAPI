package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import fr.inote.inoteApi.ConstantsForTests;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.dto.ActivationDtoRequest;
import fr.inote.inoteApi.dto.AuthenticationDtoRequest;
import fr.inote.inoteApi.dto.ChangePasswordDtoRequest;
import fr.inote.inoteApi.dto.PasswordDtoRequest;
import fr.inote.inoteApi.dto.PublicUserDtoRequest;
import fr.inote.inoteApi.dto.RefreshConnectionDtoRequest;
import fr.inote.inoteApi.dto.SignInDtoresponse;
import fr.inote.inoteApi.dto.UserDtoRequest;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.repository.ValidationRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.AUTHORIZATION;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.BEARER;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Integration tests of Endpoints
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */

/*
 * The @SpringBootTest annotation is used for integration testing in Spring Boot
 * applications.
 * It helps in bootstrapping the application context required for testing
 */
@SpringBootTest

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")
@DirtiesContext // Clean applicationContext after passed (prevent side-effects between integrations tests)
public class AuthController_IT {

        /* JUNIT5 EXTENSIONS ACCESS AS OBJECT */
        /* ============================================================ */
        // GreenMail (smtp server mocking)
        @RegisterExtension
        static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
                        .withConfiguration(GreenMailConfiguration.aConfig().withUser("duke", "springboot"))
                        .withPerMethodLifecycle(false);

        /* DEPENDENCIES */
        /* ============================================================ */
        @Autowired
        private WebApplicationContext context;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtRepository jwtRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private ValidationRepository validationRepository;

        /* TEST VARIABLES */
        /* ============================================================ */
        /*
         * MockMvc provides a convenient way to send requests to your application and
         * inspect the
         * responses, allowing you to verify the behavior of your controllers in
         * isolation.
         * -> Will be configured initialized before each test
         */
        private MockMvc mockMvc;

        private User userRef = User.builder()
                        .password(REFERENCE_USER_PASSWORD)
                        .role(this.roleRef)
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .build();

        private UserDtoRequest userDtoRequest = new UserDtoRequest(REFERENCE_USER_NAME, REFERENCE_USER_EMAIL,
                        REFERENCE_USER_PASSWORD);

        private Role roleRef = Role.builder()
                        .name(RoleEnum.ADMIN)
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
        void setUp() throws Exception {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                // this.bearerAuthorization = this.connectUserAndReturnBearer();
        }

        @AfterEach
        void tearDown() throws FolderException {
                // Clean database
                this.jwtRepository.deleteAll();
                this.validationRepository.deleteAll();
                this.userRepository.deleteAll();

                // Clean mailBox
                AuthController_IT.greenMail.purgeEmailFromAllMailboxes();
        }

        /* CONTROLLERS INTEGRATION TEST */
        /* ============================================================ */

        @Test
        @DisplayName("Register an non" +
                        " existing user")
        void IT_register_shouldSuccess_whenUserNotExist() throws Exception {

                /* Act & assert */
                // Send request, print response, check returned status and primary checking
                // (status code, content body type...)
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andExpect(MockMvcResultMatchers.content()
                                                .string(MessagesEn.ACTIVATION_NEED_ACTIVATION));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];
                                        assertThat(GreenMailUtil.getBody(receivedMessage))
                                                        .contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });
        }

        @Test
        @DisplayName("Activate an user with good code")
        void IT_activation_ShouldSuccess_whenCodeIsCorrect() throws Exception {

                /* Arrange */
                final String[] messageContainingCode = new String[1];

                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isCreated());
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", extractedCode);

                /* Act & assert */
                // Send request, print response, check returned status and primary checking
                // (status code, content body type...)
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

        }

        @Test
        @DisplayName("Activate an user with bad code")
        void IT_activation_ShouldFail_whenCodeIsNotCorrect() throws Exception {
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", "BadCode");

                /* Act & assert */
                this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Sign user with good credentials")
        void IT_signIn_ShouldSuccess_whenCredentialsAreCorrect() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                /* Act */
                // Send request, print response, check returned status and content type
                AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                this.userDtoRequest.username(), this.userDtoRequest.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInDtoresponse signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInDtoresponse.class);

                /* Assert */
                assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());
        }

        @Test
        @DisplayName("Sign user with bad credentials")
        void IT_signIn_ShouldFail_whenCredentialsAreNotCorrect() throws Exception {

                /* Act & assert */
                AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                "JamesWebb@triton.com", "fjOM487$?8dd");

                this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("Change password of existing user")
        void IT_changePassword_ShouldSuccess_WhenUsernameExists() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                /* Act */
                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                this.userRef.getUsername());

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Attempt to change password of a non existing user")
        void IT_changePassword_ShouldFail_WhenUsernameNotExist() throws Exception {
                /* Act & assert */
                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                "idontexist@neant.com");
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Attempt to change password with bad formated email")
        void IT_changePassword_ShouldFail_WhenEmailIsBadFormated() throws Exception {
                /* Act & assert */
                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                "malformedEmail@@malformed.com");
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password of existing user")
        void IT_newPassword_ShouldSuccess_WhenUserExists() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                this.userDtoRequest.username());

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // activation code recuperation
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(2);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                String codeStr = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(codeStr);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                PasswordDtoRequest passwordDtoRequest = new PasswordDtoRequest(
                                this.userDtoRequest.username(),
                                extractedCode,
                                "klfbeUB22@@@?sdjfJJ");

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(passwordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content().string(MessagesEn.NEW_PASSWORD_SUCCESS));
        }

        @Test
        @DisplayName("set new password of non existing user")
        void IT_newPassword_ShouldFail_WhenUserNotExists() throws Exception {
                /* Act & assert */
                PasswordDtoRequest newPasswordDto = new PasswordDtoRequest(
                                this.validationRef.getUser().getEmail(),
                                this.validationRef.getCode(),
                                this.validationRef.getUser().getPassword());

                // Act
                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password with non-referenced validation by code")
        void IT_newPassword_ShouldFail_WhenValidationNotExists() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                this.userDtoRequest.username());

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // activation code recuperation
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(2);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                String codeStr = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(codeStr);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                PasswordDtoRequest newPasswordDto = new PasswordDtoRequest(
                                this.userDtoRequest.username(),
                                "1111111",
                                "klfbeUB22@@@?sdjfJJ");

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("set new password with not enough secured password")
        void IT_newPassword_ShouldFail_WhenPasswordIsNotEnoughSecured() throws Exception {

                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                /* Assert */
                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                ChangePasswordDtoRequest changePasswordDtoRequest = new ChangePasswordDtoRequest(
                                this.userDtoRequest.username());

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(changePasswordDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                // activation code recuperation
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(2);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                String codeStr = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(codeStr);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                PasswordDtoRequest newPasswordDto = new PasswordDtoRequest(
                                this.userDtoRequest.username(),
                                extractedCode,
                                "1234");

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Refresh connection with correct refresh token value")
        void IT_refreshConnectionWithRefreshTokenValue_ShouldSuccess_WhenRefreshTokenValueIsCorrect() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                this.userDtoRequest.username(), this.userDtoRequest.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInDtoresponse signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInDtoresponse.class);
                assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());

                /* Act */
                RefreshConnectionDtoRequest refreshConnectionDto = new RefreshConnectionDtoRequest(
                                signInDtoresponse.refresh());
                MvcResult result = this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andReturn();

                returnedResponse = result.getResponse().getContentAsString();
                signInDtoresponse = this.objectMapper.readValue(returnedResponse, SignInDtoresponse.class);

                /* Assert */
                assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(randomUUID().toString().length());
        }

        @Test
        @DisplayName("Refresh connection with bad refresh token value")
        void IT_refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenValueIsNotCorrect() throws Exception {
                /* Act & assert */
                RefreshConnectionDtoRequest refreshConnectionDtoRequest = new RefreshConnectionDtoRequest("badValue");
                this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("Signout user effectivly connected")
        void IT_signOut_ShouldSuccess_whenUserIsConnected() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                this.userDtoRequest.username(), this.userDtoRequest.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInDtoresponse signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInDtoresponse.class);
                assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());

                /* Act & assert */
                this.mockMvc.perform(
                                post(Endpoint.SIGN_OUT).header(AUTHORIZATION, BEARER+" " + signInDtoresponse.bearer()))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Signout user with bad Bearer")
        void IT_signOut_ShouldUnauthorized_whenBearerIdBad() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                this.userDtoRequest.username(), this.userDtoRequest.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInDtoresponse signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInDtoresponse.class);
                assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_OUT).header("authorization", "Bearer "
                                + "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYW5nb2t1QGthbWUtaG91c2UuY29tIiwibmFtZSI6InNhbmdva3UiLCJleHAiOjE3MTI3NDYzOTJ9.QioVM3zc4yrFaZXadV0DQ5UiW_UrlcX83wm_cgKi0Dw"))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Register an existing user in database")
        void IT_register_shouldFail_whenUserExist() throws Exception {
                /* Arrange */
                assertThatCode(() -> {
                        Role role = this.roleRepository.findByName(RoleEnum.USER)
                                        .orElseThrow(InoteUserException::new);
                        this.userRef.setRole(role);
                        this.userRepository.save(this.userRef);
                }).doesNotThrowAnyException();

                /* Act & assert */
                // Send request, print response, check returned status and primary checking
                // (status code, content body type...)
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }

        @Test
        @DisplayName("Get the current user when he is connected")
        void IT_getCurrentUser_shouldSuccess_whenUserIsConnected() throws JsonProcessingException, Exception {

                /* Arrange */
                String bearer = this.connectAndReturnBearer();

                /* Act & assert */
                ResultActions response = this.mockMvc.perform(
                                MockMvcRequestBuilders.get(Endpoint.GET_CURRENT_USER)
                                                .header("Authorization", BEARER + " "+ bearer)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                PublicUserDtoRequest currentUser = mapper.readValue(returnedResponse,PublicUserDtoRequest.class);
                              

                assertThat(currentUser.pseudo()).isEqualTo(this.userRef.getName());
                assertThat(currentUser.username()).isEqualTo(this.userRef.getUsername());
        }

        @Test
        @DisplayName("Attempt to get the current user when he is not connected")
        void IT_getCurrentUser_shouldReturnForbidden_whenBearerIsNotPresent() throws Exception {
                /* Act & assert */
                this.mockMvc.perform(
                                MockMvcRequestBuilders.get(Endpoint.GET_CURRENT_USER)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Attempt to get the current user when he is not connected")
        void IT_getCurrentUser_shouldReturnForbidden_whenBearerIsNotCorrect() throws Exception {
                /* Act & assert */
                this.mockMvc.perform(
                                MockMvcRequestBuilders.get(Endpoint.GET_CURRENT_USER)
                                                .header("authorization", "Bearer " + ConstantsForTests.FALSE_BEARER)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        private String connectAndReturnBearer() throws JsonProcessingException, Exception {
                 /* Arrange */
                 final String[] messageContainingCode = new String[1];
                 this.mockMvc.perform(
                                 post(Endpoint.REGISTER)
                                                 .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                 .content(this.objectMapper.writeValueAsString(this.userDtoRequest)));
                 await()
                                 .atMost(2, SECONDS)
                                 .untilAsserted(() -> {
                                         MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                         assertThat(receivedMessages.length).isEqualTo(1);
 
                                         MimeMessage receivedMessage = receivedMessages[0];
 
                                         messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                         assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                 });
 
                 final String reference = "activation code : ";
                 int startSubtring = messageContainingCode[0].indexOf(reference);
                 int startIndexOfCode = startSubtring + reference.length();
                 int endIndexOfCode = startIndexOfCode + 6;
                 String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                 ActivationDtoRequest activationDtoRequest = new ActivationDtoRequest(extractedCode);
                 ResultActions response = this.mockMvc.perform(
                                 post(Endpoint.ACTIVATION)
                                                 .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                 .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                 .andExpect(MockMvcResultMatchers.status().isOk());
 
                 String returnedResponse = response.andReturn().getResponse().getContentAsString();
 
                 assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);
 
                 AuthenticationDtoRequest authenticationDtoRequest = new AuthenticationDtoRequest(
                                 this.userDtoRequest.username(), this.userDtoRequest.password());
 
                 response = this.mockMvc.perform(
                                 post(Endpoint.SIGN_IN)
                                                 .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                 .content(this.objectMapper
                                                                 .writeValueAsString(authenticationDtoRequest)))
                                 .andExpect(MockMvcResultMatchers.status().isOk())
                                 .andExpect(content().contentType(MediaType.APPLICATION_JSON));
 
                 returnedResponse = response.andReturn().getResponse().getContentAsString();
                 SignInDtoresponse signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                 SignInDtoresponse.class);
                 assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                 assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());
                return signInDtoresponse.bearer();
        }
}
