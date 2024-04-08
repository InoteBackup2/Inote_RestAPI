package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
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
    private RoleRepository roleRepository;

    @Autowired
    private ValidationRepository validationRepository;

    @Autowired
    private NotificationServiceImpl notificationService;

    private User userRef;

    private UserDto userDtoRef;
    ;

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
                    Role role = this.roleRepository.findByName(RoleEnum.USER).orElseThrow(() -> new InoteUserException("Role not found in database"));
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
}
