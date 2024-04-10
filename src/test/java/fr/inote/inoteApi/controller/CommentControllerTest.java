package fr.inote.inoteApi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.constants.MessagesEn;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.*;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.*;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.entity.Validation;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.service.impl.CommentServiceImpl;
import fr.inote.inoteApi.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;
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


    private CommentDto commentDtoRef;

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
        this.commentDtoRef = new CommentDto("Application should provide most functionalities");

        this.commentRef = Comment.builder()
                .id(1)
                .message(this.commentDtoRef.msg())
                .build();
    }

    @Test
    @DisplayName("Create a comment with message not empty")
    void create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {
        // Arrange
        when(this.commentService.createComment(any(String.class))).thenReturn(this.commentRef);

        // Send request, print response, check returned status and content type
        ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(this.commentDtoRef)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // Get serialized results
        MvcResult result = response.andReturn();
        String contentAsString = result.getResponse().getContentAsString();

        // Deserialization results
        Comment returnedComment = this.objectMapper.readValue(contentAsString, Comment.class);

        /*Assert*/
        assertThat(returnedComment).isEqualTo(this.commentRef);
    }

    @Test
    @DisplayName("Create a comment with message empty or blank")
    void create_shouldFail_whenMessageIsEmptyOrBlank() throws Exception {
        // Arrange
        when(this.commentService.createComment(any(String.class))).thenThrow(InoteEmptyMessageCommentException.class);
        CommentDto commentDto_empty = new CommentDto("");
        CommentDto commentDto_blank = new CommentDto("      ");

        // Act & assert
        ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_empty)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());

        response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(this.objectMapper.writeValueAsString(commentDto_blank)))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }
}
