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
 * Unit tests of CommentController layer
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    // test references
    private CommentDtoRequest commentDtoRequestRef;
    private Comment commentRef;
    private User userRef;

    @BeforeEach
    void init() {
        // Reference creation
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        this.userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();
        this.commentDtoRequestRef = new CommentDtoRequest("Application should provide most functionalities");
        this.commentRef = Comment.builder()
                .id(1)
                .message(this.commentDtoRequestRef.msg())
                .user(this.userRef)
                .build();
    }

    @Test
    @DisplayName("Create a comment with message not empty")
    void create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {
        // Arrange
        when(this.commentService.createComment(anyString())).thenReturn(this.commentRef);

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
    }

    @Test
    @DisplayName("Create a comment with message empty or blank")
    void create_shouldFail_whenMessageIsEmptyOrBlank() throws Exception {
        // Arrange
        when(this.commentService.createComment(anyString())).thenThrow(InoteEmptyMessageCommentException.class);
        CommentDtoRequest commentDto_Request_empty = new CommentDtoRequest("");
        CommentDtoRequest commentDto_Request_blank = new CommentDtoRequest("      ");

        // Act & assert
        ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_Request_empty)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

        response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_Request_blank)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }
}
