package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.dto.CommentDtoRequest;
import fr.inote.inoteApi.dto.CommentDtoResponse;
import fr.inote.inoteApi.dto.UserDto;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.repository.ValidationRepository;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.mail.internet.MimeMessage;

import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/*The @SpringBootTest annotation is used for integration testing in Spring Boot applications.
 * It helps in bootstrapping the application context required for testing */
@SpringBootTest

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")
public class CommentController_IT {

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
        private CommentRepository commentRepository;

        @Autowired
        private ValidationRepository validationRepository;

        // For use methods not in interface
        @Autowired
        private UserServiceImpl userServiceImpl;

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

        private CommentDtoRequest commentDtoRequestRef = new CommentDtoRequest(
                        "Application should provide most functionalities");

        private Comment commentRef = Comment.builder()
                        .id(1)
                        .message(this.commentDtoRequestRef.msg())
                        .build();
        private String bearerAuthorization;
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        User userRef = User.builder()
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .password(REFERENCE_USER_PASSWORD)
                        .role(roleForTest)
                        .build();

        private UserDto userDtoRef = new UserDto(this.userRef.getName(),
                        this.userRef.getUsername(), this.userRef.getPassword());

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void setUp() throws Exception {
                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                this.commentRepository.deleteAll();
                this.jwtRepository.deleteAll();
                this.validationRepository.deleteAll();
                this.userRepository.deleteAll();
                CommentController_IT.greenMail.purgeEmailFromAllMailboxes();
        }

        // @AfterEach
        // void tearDown() throws FolderException {}

        /* CONTROLLERS UNIT TEST */
        /* ============================================================ */
        @Test
        @DisplayName("Create a comment with message not empty")
        void IT_create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {

                /* Act */
                this.bearerAuthorization = this.connectUserAndReturnBearer();

                // Send request, print response, check returned status and primary checking
                // (status code, content body type...)
                ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(this.commentDtoRequestRef)))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                // Get serialized results
                MvcResult result = response.andReturn();
                String serializedResult = result.getResponse().getContentAsString();

                // Deserialization results
                CommentDtoResponse returnedComment = this.objectMapper.readValue(serializedResult,
                                CommentDtoResponse.class);

                /* Assert */
                assertThat(returnedComment.message()).isEqualTo(this.commentRef.getMessage());
        }

        @Test
        @DisplayName("Create a comment with message empty or blank")
        void create_shouldFail_whenMessageIsEmptyOrBlank() throws Exception {

                this.bearerAuthorization = this.connectUserAndReturnBearer();

                // Act & assert
                CommentDtoRequest commentDto_Request_empty = new CommentDtoRequest("");
                CommentDtoRequest commentDto_Request_blank = new CommentDtoRequest("      ");

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(commentDto_Request_empty)))
                                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(commentDto_Request_blank)))
                                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }

        @Test
        @DisplayName("Create comment with user without authorities to do this")
        void create_ShouldFail_whenUserDontHaveTheGoodAuthorities() throws JsonProcessingException, Exception {

                // Connect user with role tester, whithout permissions for this endpoint
                String bearer = this.connectTesterAndReturnBearer();

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + bearer)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(this.commentDtoRequestRef)))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Get all comments registered in database when user have  permissions")
        void getComments_ShouldSuccess_WhenUserHavePermissions() throws Exception {

                /* Arrange */
                this.bearerAuthorization = this.connectUserAndReturnBearer();

                final String message1 = "this application is really crap";
                final String message2 = "What in God's name have I done to use such an application?";
                final String message3 = "I'm puzzled by this application...";

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message1))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message2))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message3))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                /* Act & assert */
                MvcResult result = this.mockMvc.perform(get(Endpoint.COMMENT_GET_ALL)
                                .header("authorization", "Bearer " + this.bearerAuthorization))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content()
                                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andReturn();
                String returnedResponse = result.getResponse().getContentAsString();

                assertThat(returnedResponse).contains(message1);
                assertThat(returnedResponse).contains(message2);
                assertThat(returnedResponse).contains(message3);

        }

        @Test
        @DisplayName("Get all comments registered in database when user don't have  permissions")
        void getComments_ShouldBeForbidden_WhenUserNotHavePermisssions() throws Exception {

                /* Arrange */
                this.bearerAuthorization = this.connectUserAndReturnBearer();

                final String message1 = "this application is really crap";
                final String message2 = "What in God's name have I done to use such an application?";
                final String message3 = "I'm puzzled by this application...";

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message1))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message2))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .header("authorization", "Bearer " + this.bearerAuthorization)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new CommentDtoRequest(message3))))
                                .andExpect(MockMvcResultMatchers.status().isCreated());

                /* Act & assert */
                CommentController_IT.greenMail.purgeEmailFromAllMailboxes();
                this.jwtRepository.deleteAll();
                this.validationRepository.deleteAll();
                this.bearerAuthorization = this.connectTesterAndReturnBearer();

                this.mockMvc.perform(get(Endpoint.COMMENT_GET_ALL)
                                .header("authorization", "Bearer " + this.bearerAuthorization))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        /* UTILS */
        /* ============================================================ */

        /**
         * Connect an user to application and return token value
         *
         * @return token value
         * @throws Exception when anomaly occurs
         * @date 11/04/2024
         * @author AtsuhikoMochizuki
         */
        private String connectUserAndReturnBearer() throws Exception {
                final String[] messageContainingCode = new String[1];
                this.mockMvc.perform(
                                post(Endpoint.REGISTER)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(this.userDtoRef)));
                await()
                                .atMost(5, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubstring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubstring + reference.length();
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
                String returnedMsg = JsonPath.parse(returnedResponse).read("$.msg");
                assertThat(returnedMsg).isEqualTo(MessagesEn.ACTIVATION_OF_USER_OK);

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

                returnedResponse = response.andReturn().getResponse().getContentAsString();
                String bearer = JsonPath.parse(returnedResponse).read("$.bearer");
                assertThat(bearer.length()).isEqualTo(145);

                return bearer;
        }

        /**
         * Connect a tester (authenticable, but without permissions) to application and
         * return token value
         *
         * @return token value
         * @throws Exception when anomaly occurs
         * @date 11/04/2024
         * @author AtsuhikoMochizuki
         */
        private String connectTesterAndReturnBearer() throws Exception {

                final String[] messageContainingCode = new String[1];
                Role roleForTest = Role.builder().name(RoleEnum.TESTER).build();
                User anotherUser = User.builder()
                                .email(REFERENCE_USER2_EMAIL)
                                .name(REFERENCE_USER2_NAME)
                                .password(REFERENCE_USER2_PASSWORD)
                                .role(roleForTest)
                                .build();
                UserDto anotherUserDto = new UserDto(anotherUser.getName(), anotherUser.getUsername(),
                                anotherUser.getPassword());

                // No Endpoint for this method
                this.userServiceImpl.registerTester(anotherUser);

                await()
                                .atMost(5, SECONDS)
                                .untilAsserted(() -> {
                                        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
                                        assertThat(receivedMessages.length).isEqualTo(1);

                                        MimeMessage receivedMessage = receivedMessages[0];

                                        messageContainingCode[0] = GreenMailUtil.getBody(receivedMessage);
                                        assertThat(messageContainingCode[0]).contains(EMAIL_SUBJECT_ACTIVATION_CODE);
                                });

                final String reference = "activation code : ";
                int startSubstring = messageContainingCode[0].indexOf(reference);
                int startIndexOfCode = startSubstring + reference.length();
                int endIndexOfCode = startIndexOfCode + 6;
                String extractedCode = messageContainingCode[0].substring(startIndexOfCode, endIndexOfCode);
                Map<String, String> bodyRequest = new HashMap<>();
                bodyRequest.put("code", extractedCode);

                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.ACTIVATION)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper.writeValueAsString(bodyRequest)));
                response
                                .andExpect(MockMvcResultMatchers.status().isOk());

                Map<String, String> signInBodyContent = new HashMap<>();
                signInBodyContent.put("username", anotherUserDto.username());
                signInBodyContent.put("password", anotherUserDto.password());

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

                return bearer;
        }
}
