package fr.inote.inoteApi.integrationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.ProtectedUserResponseDto;
import fr.inote.inoteApi.dto.SignInRequestDto;
import fr.inote.inoteApi.dto.PublicUserResponseDto;
import fr.inote.inoteApi.dto.SignInResponseDto;
import fr.inote.inoteApi.dto.UserRequestDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.ValidationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.BEARER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests of Endpoints related to users
 *
 * @author atsuhikoMochizuki
 * @date 2024-06-11
 */

@SpringBootTest
// @ActiveProfiles("test")
// @DirtiesContext
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
        private JwtRepository jwtRepository;

        @Autowired
        private JwtServiceImpl jwtService;

        @Autowired
        private ValidationRepository validationRepository;

        /* TEST VARIABLES */
        /* ============================================================ */
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
                        .actif(true)
                        .build();

        private SignInRequestDto adminSignInRequestDto = new SignInRequestDto(REFERENCE_WITH_ADMIN_ROLE_EMAIL,
                        REFERENCE_WITH_ADMIN_ROLE_PASSWORD);
        private SignInRequestDto userSignInRequestDto = new SignInRequestDto(REFERENCE_WITH_USER_ROLE_EMAIL,
                        REFERENCE_WITh_USER_ROLE_PASSWORD);

        final String ENCRYPTION_KEY_FOR_TEST = "40c9201ff1204cfaa2b8eb5ac72bbe5020af8dfaa3b59cf243a5d41e04fb6b1907c490ef0686e646199d6629711cbccd953e11df4bbd913da2a8902f57e99a55";

        List<User> users = new ArrayList<>();
        List<ProtectedUserResponseDto> protectedUserDtos = new ArrayList<>();

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void setUp() throws Exception {

                this.mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                this.jwtService.setValidityTokenTimeInSeconds(1800);
                this.jwtService.setAdditionalTimeForRefreshTokenInSeconds(1000);
                this.jwtService.setEncryptionKey(ENCRYPTION_KEY_FOR_TEST);
        }

        @AfterEach
        void tearDown() throws FolderException {
                // Clean database
                this.jwtRepository.deleteAll();
                this.validationRepository.deleteAll();

                // Clean mailBox
                UserController_IT.greenMail.purgeEmailFromAllMailboxes();
        }

        /* CONTROLLERS INTEGRATION TEST */
        /* ============================================================ */
        @Test
        @DisplayName("Get by username an existing user when my role is ADMIN")
        void IT_getUser_ShouldSuccess_WhenUserExistsAndSenderIsAdmin() throws Exception {

                /* Act */
                SignInResponseDto credentials = this.connectAnAdminAndReturnAllCredentials();

                ResultActions response = this.mockMvc.perform(post(Endpoint.USER)
                                .header("Authorization", BEARER + " " + credentials.bearer())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(
                                                new UserRequestDto(this.adminSignInRequestDto.username()))))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                /* Assert */
                String serializedResponse = response.andReturn().getResponse().getContentAsString();
                PublicUserResponseDto parsedResponse = this.objectMapper.readValue(serializedResponse,
                                new TypeReference<PublicUserResponseDto>() {
                                });

                assertThat(parsedResponse.username()).isEqualTo(this.adminSignInRequestDto.username());
        }

        @Test
        @DisplayName("Get by username an existing user when my role is USER")
        void IT_getUser_ShouldFail_WhenUserExistsAndSenderIsNotAdmin() throws Exception {

                /* Act */
                SignInResponseDto credentials = this.connectAnUserAndReturnAllCredentials();

                this.mockMvc.perform(post(Endpoint.USER)
                                .header("Authorization", BEARER + " " + credentials.bearer())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(
                                                new UserRequestDto(this.adminSignInRequestDto.username()))))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Get by username an non-existing user")
        void getUser_ShouldFailed_WhenUserNotExists() throws Exception {

                /* Act & assert */
                SignInResponseDto credentials = this.connectAnAdminAndReturnAllCredentials();
                this.mockMvc.perform(post(Endpoint.USER)
                                .header("Authorization", BEARER + " " + credentials.bearer())
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(new UserRequestDto("dende@namek.org"))))
                                .andExpect(status().isNotFound())
                                .andExpect(result -> assertTrue(
                                                result.getResolvedException() instanceof UsernameNotFoundException));
        }

        @Test
        @DisplayName("Get by username an existing user when bearer is not present")
        void getUser_ShouldFailed_WhenBearerIsOmitted() throws Exception {

                /* Act */
                this.mockMvc.perform(post(Endpoint.USER)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(this.adminSignInRequestDto)))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Get all users when at least one is present and my role is ADMIN")
        void list_ShouldSuccess_WhenAnUserExistsAndSenderIsAdmin() throws Exception {

                /* Arrange */
                this.protectedUserDtos.clear();
                protectedUserDtos = new ArrayList<>();
                this.protectedUserDtos.add(new ProtectedUserResponseDto(
                                this.userRef.getPseudonyme(),
                                this.userRef.getEmail(),
                                this.userRef.isActif(),
                                this.userRef.getPseudonyme(),
                                this.userRef.getAvatar(),
                                this.userRef.getRole().getName().toString()));

                /* Act & assert */
                SignInResponseDto credentials = this.connectAnAdminAndReturnAllCredentials();
                ResultActions response = this.mockMvc.perform(get(Endpoint.GET_ALL_USERS)
                                .header("Authorization", BEARER + " " + credentials.bearer()))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                String serializedResponse = response.andReturn().getResponse().getContentAsString();
                List<ProtectedUserResponseDto> parsedResponse = this.objectMapper.readValue(serializedResponse,
                                new TypeReference<List<ProtectedUserResponseDto>>() {
                                });

                assertThat(parsedResponse).isNotEmpty();
                assertThat(parsedResponse).isNotNull();

        }

        @Test
        @DisplayName("Get all users when at least one is present and my role is USER")
        void list_ShouldFail_WhenAnUserExistsAndSenderIsUser() throws Exception {
                /* Act & assert */
                SignInResponseDto credentials = this.connectAnUserAndReturnAllCredentials();
                this.mockMvc.perform(get(Endpoint.GET_ALL_USERS)
                                .header("Authorization", BEARER + " " + credentials.bearer()))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());
        }

        @Test
        @DisplayName("Get all users when bearer is ommited")
        void list_ShouldReturnForbidden_WhenBearerIsOmmited() throws Exception {

                /* Act & assert */
                this.mockMvc.perform(get(Endpoint.GET_ALL_USERS))
                                .andExpect(MockMvcResultMatchers.status().isForbidden());

        }

        /* UTILS */
        /* ============================================================ */
        private SignInResponseDto connectAnAdminAndReturnAllCredentials() throws JsonProcessingException, Exception {
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(this.adminSignInRequestDto)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInResponseDto signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInResponseDto.class);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());

                return signInDtoresponse;
        }

        private SignInResponseDto connectAnUserAndReturnAllCredentials() throws JsonProcessingException, Exception {
                ResultActions response = this.mockMvc.perform(
                                post(Endpoint.SIGN_IN)
                                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                .content(this.objectMapper
                                                                .writeValueAsString(this.userSignInRequestDto)))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                String returnedResponse = response.andReturn().getResponse().getContentAsString();
                SignInResponseDto signInDtoresponse = this.objectMapper.readValue(returnedResponse,
                                SignInResponseDto.class);
                assertThat(signInDtoresponse.refresh().length()).isEqualTo(UUID.randomUUID().toString().length());

                return signInDtoresponse;
        }
}
