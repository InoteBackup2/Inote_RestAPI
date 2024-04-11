package fr.inote.inoteApi.integrationTest;

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

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.ConstantsForTests.REFERENCE_USER_PASSWORD;
import static fr.inote.inoteApi.crossCutting.constants.MessagesEn.EMAIL_SUBJECT_ACTIVATION_CODE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("test")
public class CommentController_IT {
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

    private CommentDtoRequest commentDtoRequestRef;
    private Comment commentRef;

    private User userRef;

    private UserDto userDtoRef;

    private Role roleRef;
    private Validation validationRef;

    private String bearerAuthorization;

    @BeforeEach
    void setUp() throws Exception {
        CommentController_IT.greenMail.purgeEmailFromAllMailboxes();
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

        this.commentDtoRequestRef = new CommentDtoRequest("Application should provide most functionalities");
        this.commentRef = Comment.builder()
                .id(1)
                .message(this.commentDtoRequestRef.msg())
                .build();

        bearerAuthorization = connectUserAndReturnBearer();
        System.out.println(bearerAuthorization);
    }

    @Test
    @DisplayName("Create a comment with message not empty")
    void IT_create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {
        // Act
        ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .header("authorization", "Bearer " + this.bearerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.commentDtoRequestRef)))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // Get serialized results
        MvcResult result = response.andReturn();
        String serializedResult = result.getResponse().getContentAsString();

        // Deserialization results
        CommentDtoResponse returnedComment = this.objectMapper.readValue(serializedResult, CommentDtoResponse.class);

        /*Assert*/
        assertThat(returnedComment.message()).isEqualTo(this.commentRef.getMessage());
    }

    public String connectUserAndReturnBearer() throws Exception {
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

        return bearer;
    }
}
