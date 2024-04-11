package fr.inote.inoteApi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.*;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.service.impl.CommentServiceImpl;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Unit tests of Controller layer
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */
/* Dedicated for unit test mvc controllers:
 * -disable full-auto configuration
 * -apply config only relevant mvc tests
 * -autoconfigure mockMvc instance (used to test the dispatcher servlet and your controllers)
 */
@WebMvcTest(CommentController.class)

/* Enables all autoconfiguration related to MockMvc and ONLY MockMvc + none Spring security filters applied*/
@AutoConfigureMockMvc(addFilters = false)

/* Add Mockito functionalities to Junit 5*/
@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {

    /* DEPENDENCIES INJECTION*/
    /*============================================================*/

    /* MockMvc provides a convenient way to send requests to your application and inspect the
     * responses, allowing you to verify the behavior of your controllers in isolation.
     * -> Need to be autowired to be autoconfigured */
    @Autowired
    private MockMvc mockMvc;

    /* ObjectMapper provide functionalities for read and write JSON data's*/
    @Autowired
    private ObjectMapper objectMapper;

    /* DEPENDENCIES MOCKING */
    /*============================================================*/
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtServiceImpl jwtServiceImpl;
    @MockBean
    private UserServiceImpl userService;
    @MockBean
    private CommentRepository commentRepository;
    @MockBean
    private CommentServiceImpl commentService;

    /* REFERENCES FOR MOCKING*/
    /*============================================================*/
    private CommentDtoRequest commentDtoRequestRef;
    private Comment commentRef;

    /* FIXTURES */
    /*============================================================*/
    @BeforeEach
    void init() {
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        User userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();
        this.commentDtoRequestRef = new CommentDtoRequest("Application should provide most functionalities");
        this.commentRef = Comment.builder()
                .id(1)
                .message(this.commentDtoRequestRef.msg())
                .user(userRef)
                .build();
    }

    /* CONTROLLERS UNIT TEST */
    /*============================================================*/
    @Test
    @DisplayName("Create a comment with message not empty")
    void create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {
        /*Arrange*/
        when(this.commentService.createComment(anyString())).thenReturn(this.commentRef);

        /*Act*/
        // Send request, print response, check returned status and content type
        ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.commentDtoRequestRef)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // Get serialized results
        MvcResult result = response.andReturn();
        String contentAsString = result.getResponse().getContentAsString();

        // Deserialization results
        CommentDtoResponse returnedComment = this.objectMapper.readValue(contentAsString, CommentDtoResponse.class);

        /*Assert*/
        assertThat(returnedComment.message()).isEqualTo(this.commentRef.getMessage());

        /*Mocking invocation check*/
        verify(this.commentService, times(1)).createComment(anyString());
    }

    @Test
    @DisplayName("Create a comment with message empty or blank")
    void create_shouldFail_whenMessageIsEmptyOrBlank() throws Exception {
        /*Arrange*/
        when(this.commentService.createComment(anyString())).thenThrow(InoteEmptyMessageCommentException.class);

        /*Act & assert*/
        CommentDtoRequest commentDto_Request_empty = new CommentDtoRequest("");
        CommentDtoRequest commentDto_Request_blank = new CommentDtoRequest("      ");
        this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_Request_empty)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

        this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_Request_blank)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

        /*Mocking invocation check*/
        verify(this.commentService, times(2)).createComment(anyString());
    }
}
