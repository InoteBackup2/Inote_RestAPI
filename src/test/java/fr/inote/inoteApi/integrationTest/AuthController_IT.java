package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;

import fr.inote.inoteApi.ConstantsForTests;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.dto.NewPasswordDto;
import fr.inote.inoteApi.dto.PublicUserDto;
import fr.inote.inoteApi.dto.RefreshConnectionDto;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.repository.ValidationRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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

        private UserDto userDtoRef = new UserDto(REFERENCE_USER_NAME, REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);

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
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> map = mapper.readValue(returnedResponse, Map.class);
                assertThat(map.size()).isEqualTo(1);
                assertThat(map.get("msg")).isEqualTo(MessagesEn.ACTIVATION_NEED_ACTIVATION);

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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                Map<String, String> attemptedResponse = new HashMap<>();
                attemptedResponse.put("msg", MessagesEn.ACTIVATION_OF_USER_OK);

                /* Assert */
                assertThat(returnedResponse).isEqualTo(this.objectMapper.writeValueAsString(attemptedResponse));

        }

        @Test
        @DisplayName("Activate an user with bad code")
        void IT_activation_ShouldFail_whenCodeIsNotCorrect() throws Exception {
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", "BadCode");

                /* Act & assert */
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                Map<String, String> attemptedResponse = new HashMap<>();
                attemptedResponse.put("msg", new InoteValidationNotFoundException().getMessage());

                /* Assert */
                assertThat(returnedResponse).isEqualTo(this.objectMapper.writeValueAsString(attemptedResponse));
        }

        @Test
        @DisplayName("Sign user with good credentials")
        void IT_signIn_ShouldSuccess_whenCredentialsAreCorrect() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)));
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                Map<String, String> attemptedResponse = new HashMap<>();
                attemptedResponse.put("msg", MessagesEn.ACTIVATION_OF_USER_OK);

                /* Assert */
                assertThat(returnedResponse).isEqualTo(this.objectMapper.writeValueAsString(attemptedResponse));

                /* Act */
                // Send request, print response, check returned status and content type
                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", this.userDtoRef.username());
                signInBodyContent.put("password", this.userDtoRef.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                String refresh = JsonPath.parse(returnedResponse).read("$.refresh");

                /* Assert */
                assertThat(bearer.length()).isEqualTo(145);
                assertThat(refresh.length()).isEqualTo(UUID.randomUUID().toString().length());
        }

        @Test
        @DisplayName("Sign user with bad credentials")
        void IT_signIn_ShouldFail_whenCredentialsAreNotCorrect() throws Exception {

                /* Act & assert */
                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", "JamesWebb@triton.com");
                signInBodyContent.put("password", "fjOM487$?8dd");

                this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> attemptedResponse = new HashMap<>();
                attemptedResponse.put("msg", MessagesEn.ACTIVATION_OF_USER_OK);

                assertThat(returnedResponse).isEqualTo(this.objectMapper.writeValueAsString(attemptedResponse));

                /* Act */
                // Send request, print response, check returned status and content type
                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", this.userDtoRef.username());

                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
        }

        @Test
        @DisplayName("Attempt to change password of a non existing user")
        void IT_changePassword_ShouldFail_WhenUsernameNotExist() throws Exception {
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
        void IT_newPassword_ShouldSuccess_WhenUserExists() throws Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", extractedCode);

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", this.userDtoRef.username());
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
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

                reference = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(reference);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.userDtoRef.username(),
                                extractedCode,
                                "klfbeUB22@@@?sdjfJJ");

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content().string(MessagesEn.NEW_PASSWORD_SUCCESS));
        }

        @Test
        @DisplayName("set new password of non existing user")
        void IT_newPassword_ShouldFail_WhenUserNotExists() throws Exception {
                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)));

                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", extractedCode);

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", this.userDtoRef.username());
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(2);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                reference = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(reference);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.userDtoRef.username(),
                                "1111111",
                                "klfbeUB22@@@?sdjfJJ");

                this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("set new password with not enough secured password")
        void IT_newPassword_ShouldFail_WhenPasswordIsNotEnoughSecured() throws Exception {

                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                String reference = "activation code : ";
                int startSubtring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubtring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", extractedCode);

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> usernameMap = new HashMap<>();
                usernameMap.put("email", this.userDtoRef.username());
                this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(usernameMap)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                await()
                                .atMost(2, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(2);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                reference = "activation code : ";
                startSubtring = messageContainingCode[0].indexOf(reference);
                startIndexOfCode = startSubtring + reference.length();
                endIndexOfCode = startIndexOfCode + 6;
                extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);

                /* Act & assert */
                NewPasswordDto newPasswordDto = new NewPasswordDto(
                                this.userDtoRef.username(),
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", this.userDtoRef.username());
                signInBodyContent.put("password", this.userDtoRef.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                MvcResult mvcResult = response.andReturn();

                returnedResponse = mvcResult.getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                String refresh = JsonPath.parse(returnedResponse).read("$.refresh");
                assertThat(bearer.length()).isEqualTo(145);
                assertThat(refresh.length()).isEqualTo(randomUUID().toString().length());

                /* Act */
                RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto(refresh);
                MvcResult result = this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andReturn();

                returnedResponse = result.getResponse().getContentAsString();
                bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                refresh = JsonPath.parse(returnedResponse).read("$.refresh");

                /* Assert */
                assertThat(bearer.length()).isEqualTo(145);
                assertThat(refresh.length()).isEqualTo(randomUUID().toString().length());
        }

        @Test
        @DisplayName("Refresh connection with bad refresh token value")
        void IT_refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenValueIsNotCorrect() throws Exception {
                /* Act & assert */
                RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto("badValue");
                this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)))
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", this.userDtoRef.username());
                signInBodyContent.put("password", this.userDtoRef.password());

                MvcResult result = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.bearer").isNotEmpty())
                                .andExpect(jsonPath("$.refresh").isNotEmpty())
                                .andReturn();

                returnedResponse = result.getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                assertThat(bearer.length()).isEqualTo(145);

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_OUT).header("authorization", "Bearer " + bearer))
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", this.userDtoRef.username());
                signInBodyContent.put("password", this.userDtoRef.password());

                MvcResult result = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.bearer").isNotEmpty())
                                .andExpect(jsonPath("$.refresh").isNotEmpty())
                                .andReturn();

                returnedResponse = result.getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                assertThat(bearer.length()).isEqualTo(145);

                /* Act & assert */
                this.mockMvc.perform(post(Endpoint.SIGN_OUT).header("authorization", "Bearer "
                                + "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYW5nb2t1QGthbWUtaG91c2UuY29tIiwibmFtZSI6InNhbmdva3UiLCJleHAiOjE3MTI3NDYzOTJ9.QioVM3zc4yrFaZXadV0DQ5UiW_UrlcX83wm_cgKi0Dw"))
                                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Disabled // Fail when launch ALL tests
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
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
                                .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

          @Test
        @DisplayName("Get the current user when he is connected")
        void IT_getCurrentUser_shouldSuccess_whenUserIsConnected() throws JsonProcessingException, Exception {

                /* Arrange */
                String bearer = this.connectAndReturnBearer();

                /* Act & assert */
                ResultActions response = this.mockMvc.perform(
                                MockMvcRequestBuilders.get(Endpoint.GET_CURRENT_USER)
                                                .header("authorization", "Bearer " + bearer)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, PublicUserDto> map = mapper.readValue(returnedResponse,
                                new TypeReference<Map<String, PublicUserDto>>() {
                                });
                PublicUserDto currentUser = map.get("data");
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
                                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        private String connectAndReturnBearer() throws JsonProcessingException, Exception {
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)))
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

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                Map<String, String> parsedResponse = this.objectMapper.readValue(returnedResponse,
                                new TypeReference<Map<String, String>>() {
                                });
                assertThat(parsedResponse.get("msg")).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", this.userDtoRef.username());
                signInBodyContent.put("password", this.userDtoRef.password());

                MvcResult result = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(signInBodyContent)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.bearer").isNotEmpty())
                                .andExpect(jsonPath("$.refresh").isNotEmpty())
                                .andReturn();

                returnedResponse = result.getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                assertThat(bearer.length()).isEqualTo(145);
                return bearer;
        }
}
