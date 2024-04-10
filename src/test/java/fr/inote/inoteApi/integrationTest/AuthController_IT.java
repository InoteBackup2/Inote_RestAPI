package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteValidationNotFoundException;
import fr.inote.inoteApi.dto.NewPasswordDto;
import fr.inote.inoteApi.dto.RefreshConnectionDto;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.repository.ValidationRepository;
import fr.inote.inoteApi.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
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
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

//@SpringBootTest(
//                // Loads a web application context and provides a mock web environment.
//                // It doesn’t load a real http server, just mocks the entire web server
//                // behavior.
//                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//
//                // Instantiate users for test
//                classes = SpringSecurityWebAuxTestConfig.class
//)
@SpringBootTest
//@AutoConfigureMockMvc
//@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class AuthController_IT {

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("duke", "springboot"))
            .withPerMethodLifecycle(false);

    private MockMvc mockMvc;

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

    @Autowired
    private NotificationServiceImpl notificationService;

    private User userRef;

    private UserDto userDtoRef;

    private Role roleRef;
    private Validation validationRef;

    @BeforeEach
    void setUp() throws FolderException {

        AuthController_IT.greenMail.purgeEmailFromAllMailboxes();


        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        this.roleRef = Role.builder()
                .name(RoleEnum.ADMIN)
                .build();

        this.userRef = User.builder()
                .password(REFERENCE_USER_PASSWORD)
                .role(this.roleRef)
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .build();

        this.userDtoRef = new UserDto(REFERENCE_USER_NAME, REFERENCE_USER_EMAIL, REFERENCE_USER_PASSWORD);

        this.validationRef = Validation.builder()
                .code("123456")
                .user(this.userRef)
                .creation(Instant.now())
                .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();

        this.jwtRepository.deleteAll();
        this.validationRepository.deleteAll();
        this.userRepository.deleteAll();
    }

    @Test
    //@WithMockUser
    @DisplayName("Register an non" +
            " existing user")
    void IT_register_shouldSuccess_whenUserNotExist() throws Exception {
        // Arrange
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
                .andExpect(content()
                        .string(MessagesEn.REGISTER_OK_MAIL_SENDED));
        await()
                .atMost(2, SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                    assertThat(receivedMessages.length).isEqualTo(1);

                    MimeMessage receivedMessage = receivedMessages[0];
                    assertThat(GreenMailUtil.getBody(receivedMessage)).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                });

    }

    @Test
    //@WithMockUser
    @DisplayName("Register an existing user in database")
    void IT_register_shouldFail_whenUserExist() throws Exception {
        // Arrange
        UserDto userDto = new UserDto(
                this.userRef.getName(),
                this.userRef.getUsername(),
                this.userRef.getPassword());
        assertThatCode(() -> {
                    Role role = this.roleRepository.findByName(RoleEnum.USER).orElseThrow(() -> new InoteUserException());
                    this.userRef.setRole(role);
                    this.userRepository.save(this.userRef);
                }
        ).doesNotThrowAnyException();

        // Act
        ResultActions response = this.mockMvc.perform(
                post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(userDto)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @Test
    @DisplayName("Activate an user with good code")
    void IT_activation_ShouldSuccess_whenCodeIsCorrect() throws Exception {
        final String[] messageContainingCode = new String[1];
        // Arrange
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

        // Act
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

    }

    @Test
    @DisplayName("Activate an user with bad code")
    void IT_activation_ShouldFail_whenCodeIsNotCorrect() throws Exception {
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
    @DisplayName("Sign user with good credentials")
    void IT_signIn_ShouldSuccess_whenCredentialsAreCorrect() throws Exception {
        final String[] messageContainingCode = new String[1];
        // Arrange
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

        // Act
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        // Act
        Map<String, String> signInBodyContent = new HashMap<>();
        signInBodyContent.put("username", this.userDtoRef.username());
        signInBodyContent.put("password", this.userDtoRef.password());

        response = this.mockMvc.perform(
                post(Endpoint.SIGN_IN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(signInBodyContent)));

        //Assert
        response.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bearer").isNotEmpty())
                .andExpect(jsonPath("$.refresh").isNotEmpty());
    }

    @Test
    @DisplayName("Sign user with bad credentials")
    void IT_signIn_ShouldFail_whenCredentialsAreNotCorrect() throws Exception {
        // Act
        Map<String, String> signInBodyContent = new HashMap<>();
        signInBodyContent.put("username", "JamesWebb@triton.com");
        signInBodyContent.put("password", "fjOM487$?8dd");

        ResultActions response = this.mockMvc.perform(
                post(Endpoint.SIGN_IN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(signInBodyContent)));

        //Assert
        response.andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    @DisplayName("Change password of existing user")
    void IT_changePassword_ShouldSuccess_WhenUsernameExists() throws Exception {
        //Arrange
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
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        //Act
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", this.userDtoRef.username());
        response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));

        //Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Attempt to change password of a non existing user")
    void IT_changePassword_ShouldFail_WhenUsernameNotExist() throws Exception {

        //Arrange
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", "UnknowUser@neant.com");

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

        ResultActions response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD
        )
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));

        //Assert
        response
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("set new password of existing user")
    void IT_newPassword_ShouldSuccess_WhenUserExists() throws Exception {
        // User registration request
        final String[] messageContainingCode = new String[1];
        this.mockMvc.perform(
                post(Endpoint.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.userDtoRef)));

        // activation code recuperation by email
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

        // Activation code sending
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        // Change password request
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", this.userDtoRef.username());
        response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));
        response
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


        //Act
        NewPasswordDto newPasswordDto = new NewPasswordDto(
                this.userDtoRef.username(),
                extractedCode,
                "klfbeUB22@@@?sdjfJJ");

        response = this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(newPasswordDto)))

                // Assert
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(MessagesEn.NEW_PASSWORD_SUCCESS));
    }

    @Test
    @DisplayName("set new password of non existing user")
    void IT_newPassword_ShouldFail_WhenUserNotExists() throws Exception {
        NewPasswordDto newPasswordDto = new NewPasswordDto(
                this.validationRef.getUser().getEmail(),
                this.validationRef.getCode(),
                this.validationRef.getUser().getPassword());

        //Act
        ResultActions response = this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(newPasswordDto)))
                // Assert
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("set new password with non-referenced validation by code")
    void IT_newPassword_ShouldFail_WhenValidationNotExists() throws Exception {
        // User registration request
        final String[] messageContainingCode = new String[1];
        this.mockMvc.perform(
                post(Endpoint.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.userDtoRef)));

        // activation code recuperation by email
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

        // Activation code sending
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        // Change password request
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", this.userDtoRef.username());
        response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));
        response
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


        //Act
        NewPasswordDto newPasswordDto = new NewPasswordDto(
                this.userDtoRef.username(),
                "1111111",
                "klfbeUB22@@@?sdjfJJ");

        response = this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(newPasswordDto)))

                // Assert
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("set new password with not enough secured password")
    void IT_newPassword_ShouldFail_WhenPasswordIsNotEnoughSecured() throws Exception {
        // User registration request
        final String[] messageContainingCode = new String[1];
        this.mockMvc.perform(
                post(Endpoint.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.userDtoRef)));

        // activation code recuperation by email
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

        // Activation code sending
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        // Change password request
        Map<String, String> usernameMap = new HashMap<>();
        usernameMap.put("email", this.userDtoRef.username());
        response = this.mockMvc.perform(post(Endpoint.CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(usernameMap)));
        response
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


        //Act
        NewPasswordDto newPasswordDto = new NewPasswordDto(
                this.userDtoRef.username(),
                extractedCode,
                "1234");

        response = this.mockMvc.perform(post(Endpoint.NEW_PASSWORD)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(newPasswordDto)))

                // Assert
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Refresh connection with correct refresh token value")
    void IT_refreshConnectionWithRefreshTokenValue_ShouldSuccess_WhenRefreshTokenValueIsCorrect() throws Exception {
        // Arrange
        final String[] messageContainingCode = new String[1];
        // User registration request
        this.mockMvc.perform(
                post(Endpoint.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.userDtoRef)));
        // Activation code recuperation
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

        // Account activation
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));


        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));

        // connection
        Map<String, String> signInBodyContent = new HashMap<>();
        signInBodyContent.put("username", this.userDtoRef.username());
        signInBodyContent.put("password", this.userDtoRef.password());

        response = this.mockMvc.perform(
                post(Endpoint.SIGN_IN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(signInBodyContent)));

        MvcResult mvcResult = response.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String returnedResponse = mvcResult.getResponse().getContentAsString();
        String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
        String refresh = JsonPath.parse(returnedResponse).read("$.refresh");
        assertThat(bearer.length()).isEqualTo(145);
        assertThat(refresh.length()).isEqualTo(randomUUID().toString().length());

        // Act
        RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto(refresh);
        response = this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)));
        response
                .andExpect(MockMvcResultMatchers.status().isCreated());
        returnedResponse = response.andReturn().getResponse().getContentAsString();
        bearer = JsonPath.parse(returnedResponse).read("$.bearer");
        refresh = JsonPath.parse(returnedResponse).read("$.refresh");
        assertThat(bearer.length()).isEqualTo(145);
        assertThat(refresh.length()).isEqualTo(randomUUID().toString().length());
    }

    @Test
    @DisplayName("Refresh connection with bad refresh token value")
    void IT_refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenValueIsNotCorrect() throws Exception {

        RefreshConnectionDto refreshConnectionDto = new RefreshConnectionDto("badValue");
        ResultActions response = this.mockMvc.perform(post(Endpoint.REFRESH_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(refreshConnectionDto)));
        response
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Signout user effectivly connected")
    void IT_signOut_ShouldSuccess_whenUserIsConnected() throws Exception {
        final String[] messageContainingCode = new String[1];
        // Arrange
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

        // Act
        ResultActions response = this.mockMvc.perform(
                post(Endpoint.ACTIVATION)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        // Assert
        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));
        Map<String, String> signInBodyContent = new HashMap<>();
        signInBodyContent.put("username", this.userDtoRef.username());
        signInBodyContent.put("password", this.userDtoRef.password());

        response = this.mockMvc.perform(
                post(Endpoint.SIGN_IN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(signInBodyContent)));
        response.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bearer").isNotEmpty())
                .andExpect(jsonPath("$.refresh").isNotEmpty());

        String returnedResponse = response.andReturn().getResponse().getContentAsString();
        String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
        assertThat(bearer.length()).isEqualTo(145);

        // Act
        response = this.mockMvc.perform(post(Endpoint.SIGN_OUT).
        header("authorization", "Bearer " + bearer));

        // Assert
        response.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Signout user with bad Bearer")
    void IT_signOut_ShouldUnauthorized_whenBearerIdBad() throws Exception {
        final String[] messageContainingCode = new String[1];
        // Arrange
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
                        .content(this.objectMapper.writeValueAsString(bodyRequest)));

        response
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(MessagesEn.ACTIVATION_OF_USER_OK));
        Map<String, String> signInBodyContent = new HashMap<>();
        signInBodyContent.put("username", this.userDtoRef.username());
        signInBodyContent.put("password", this.userDtoRef.password());

        response = this.mockMvc.perform(
                post(Endpoint.SIGN_IN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(signInBodyContent)));
        response.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bearer").isNotEmpty())
                .andExpect(jsonPath("$.refresh").isNotEmpty());

        String returnedResponse = response.andReturn().getResponse().getContentAsString();
        String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
        assertThat(bearer.length()).isEqualTo(145);

        // Act
        response = this.mockMvc.perform(post(Endpoint.SIGN_OUT).
                header("authorization", "Bearer " + "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYW5nb2t1QGthbWUtaG91c2UuY29tIiwibmFtZSI6InNhbmdva3UiLCJleHAiOjE3MTI3NDYzOTJ9.QioVM3zc4yrFaZXadV0DQ5UiW_UrlcX83wm_cgKi0Dw"));

        // Assert
        response.andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
