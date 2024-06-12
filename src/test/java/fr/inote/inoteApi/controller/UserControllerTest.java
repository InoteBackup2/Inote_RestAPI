package fr.inote.inoteApi.controller;

import static fr.inote.inoteApi.ConstantsForTests.REFERENCE_USER_NAME;
import static fr.inote.inoteApi.ConstantsForTests.REFERENCE_USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.PublicUserResponseDto;
import fr.inote.inoteApi.dto.UserRequestDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

/**
 * Unit tests of User controller layer
 *
 * @author atsuhikoMochizuki
 * @date 2024-06-11
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class UserControllerTest {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */

    @Autowired
    private MockMvc mockMvc;

    /* ObjectMapper provide functionalities for read and write JSON data's */
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceImpl userService;

    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtServiceImpl jwtServiceImpl;

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

    private User userRef = User.builder()
            .email(REFERENCE_USER_NAME)
            .name(REFERENCE_USER_NAME)
            .password(REFERENCE_USER_PASSWORD)
            .role(roleForTest)
            .build();

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void init() {}

    /* CONTROLLER UNIT TEST */
    /* ============================================================ */
    @Test
    @DisplayName("Get by username an existing user")
    void getUser_ShouldSuccess_WhenUserExists() throws Exception {

        /* Arrange */
        when(this.userService.loadUserByUsername(this.userRef.getUsername()))
                .thenReturn(this.userRef);

        /* Act */
        UserRequestDto userRequestDto = new UserRequestDto(this.userRef.getUsername());

        ResultActions response = this.mockMvc.perform(post(Endpoint.USER)
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
        assertThat(parsedResponse.avatar()).isEqualTo(this.userRef.getAvatar());
        assertThat(parsedResponse.actif()).isEqualTo(this.userRef.isActif());
        assertThat(parsedResponse.role()).isEqualTo(this.userRef.getRole());
    }

    @Test
    @DisplayName("Get by username an non-existing user")
    void getUser_ShouldFailed_WhenUserNotExists() throws Exception {

        /* Arrange */
        when(this.userService.loadUserByUsername(anyString()))
                .thenThrow(UsernameNotFoundException.class);

        /* Act */
        UserRequestDto userRequestDto = new UserRequestDto("quisuije@dansqueletatjerre.fr");

        this.mockMvc.perform(post(Endpoint.USER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(this.objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UsernameNotFoundException))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}