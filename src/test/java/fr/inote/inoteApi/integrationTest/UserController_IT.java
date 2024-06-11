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
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.ActivationRequestDto;
import fr.inote.inoteApi.dto.SignInRequestDto;
import fr.inote.inoteApi.dto.CommentRequestDto;
import fr.inote.inoteApi.dto.CommentResponseDto;
import fr.inote.inoteApi.dto.PublicUserResponseDto;
import fr.inote.inoteApi.dto.RegisterRequestDto;
import fr.inote.inoteApi.dto.SignInResponseDto;
import fr.inote.inoteApi.dto.UserRequestDto;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.RoleRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.repository.ValidationRepository;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




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
@DirtiesContext 
public class UserController_IT {

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
        private JwtServiceImpl jwtService;

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

        private Role roleRef = Role.builder()
                        .name(RoleEnum.USER)
                        .build();

        private User userRef = User.builder()
                        .pseudonyme(REFERENCE_PSEUDONYME)
                        .password(REFERENCE_USER_PASSWORD)
                        .role(this.roleRef)
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .build();

        private RegisterRequestDto registerRequestDto = new RegisterRequestDto(REFERENCE_PSEUDONYME,
                        REFERENCE_USER_EMAIL,
                        REFERENCE_USER_PASSWORD);

        private Validation validationRef = Validation.builder()
                        .code("123456")
                        .user(this.userRef)
                        .creation(Instant.now())
                        .expiration(Instant.now().plus(5, ChronoUnit.MINUTES))
                        .build();
        final String ENCRYPTION_KEY_FOR_TEST = "40c9201ff1204cfaa2b8eb5ac72bbe5020af8dfaa3b59cf243a5d41e04fb6b1907c490ef0686e646199d6629711cbccd953e11df4bbd913da2a8902f57e99a55";

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void setUp() throws Exception {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                this.jwtService.setValidityTokenTimeInSeconds(1800);
                this.jwtService.setAdditionalTimeForRefreshTokenInSeconds(1000);
                this.jwtService.setEncryptionKey(ENCRYPTION_KEY_FOR_TEST);
                // this.bearerAuthorization = this.connectUserAndReturnBearer();
        }

        @AfterEach
        void tearDown() throws FolderException {
                // Clean database
                this.jwtRepository.deleteAll();
                this.validationRepository.deleteAll();
                this.userRepository.deleteAll();

                // Clean mailBox
                UserController_IT.greenMail.purgeEmailFromAllMailboxes();
        }

        /* CONTROLLERS INTEGRATION TEST */
        /* ============================================================ */
        @Test
        @DisplayName("Get by username an existing user")
        void IT_getUser_ShouldSuccess_WhenUserExists() throws Exception {

                /* Act */
                SignInResponseDto credentials = this.connectAndReturnAllCredentials();
                UserRequestDto userRequestDto = new UserRequestDto(this.userRef.getUsername());

                ResultActions response = this.mockMvc.perform(get(Endpoint.USER)
                                .header("Authorization", BEARER + " " + credentials.bearer())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(userRequestDto)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                /* Assert */
                String serializedResponse = response.andReturn().getResponse().getContentAsString();
                PublicUserResponseDto parsedResponse = this.objectMapper.readValue(serializedResponse,
                                new TypeReference<PublicUserResponseDto>() {
                                });

                assertThat(parsedResponse.pseudonyme()).isEqualTo(this.userRef.getPseudonyme());
                assertThat(parsedResponse.username()).isEqualTo(this.userRef.getUsername());
                assertThat(parsedResponse.role().getName()).isEqualTo(this.userRef.getRole().getName());
        }

        @Test
        @DisplayName("Get by username an non-existing user")
        void getUser_ShouldFailed_WhenUserNotExists() throws Exception {

                /* Act & assert */
                SignInResponseDto credentials = this.connectAndReturnAllCredentials();
                UserRequestDto userRequestDto = new UserRequestDto("quisuije@dansqueletatjerre.fr");

                this.mockMvc.perform(get(Endpoint.USER)
                                .header("Authorization", BEARER + " " + credentials.bearer())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(userRequestDto)))
                                .andExpect(status().isNotFound())
                                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UsernameNotFoundException));
        }

        /* UTILS */
        /* ============================================================ */
        private SignInResponseDto connectAndReturnAllCredentials() throws JsonProcessingException, Exception {
                /* Arrange */
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(this.registerRequestDto)));
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
                ActivationRequestDto activationDtoRequest = new ActivationRequestDto(extractedCode);
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(activationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String returnedResponse = response.andReturn().getResponse().getContentAsString();

                assertThat(returnedResponse).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

                SignInRequestDto authenticationDtoRequest = new SignInRequestDto(
                                this.registerRequestDto.username(), this.registerRequestDto.password());

                response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(authenticationDtoRequest)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInResponseDto signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInResponseDto.class);
                // assertThat(signInDtoresponse.bearer().length()).isEqualTo(145);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());
                return signInDtoresponse;
        }
}
