package fr.inote.inote_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inote.inote_api.cross_cutting.constants.Endpoint;
import fr.inote.inote_api.cross_cutting.enums.RoleEnum;
import fr.inote.inote_api.cross_cutting.exceptions.*;
import fr.inote.inote_api.cross_cutting.security.impl.JwtServiceImpl;
import fr.inote.inote_api.dto.*;
import fr.inote.inote_api.entity.Comment;
import fr.inote.inote_api.entity.Role;
import fr.inote.inote_api.entity.User;
import fr.inote.inote_api.repository.CommentRepository;
import fr.inote.inote_api.service.impl.CommentServiceImpl;
import fr.inote.inote_api.service.impl.UserServiceImpl;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static fr.inote.inote_api.ConstantsForTests.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests of Controller layer
 *
 * @author atsuhiko Mochizuki
 * @date 10/04/2024
 */
/*
 * Dedicated for unit test mvc controllers:
 * -disable full-auto configuration
 * -apply config only relevant mvc tests
 * -autoconfigure mockMvc instance (used to test the dispatcher servlet and your
 * controllers)
 */
@WebMvcTest(CommentController.class)

/*
 * Enables all autoconfiguration related to MockMvc and ONLY MockMvc + none
 * Spring security filters applied
 */
@AutoConfigureMockMvc(addFilters = false)

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class CommentControllerTest {

        /* DEPENDENCIES INJECTION */
        /* ============================================================ */

        /*
         * MockMvc provides a convenient way to send requests to your application and
         * inspect the
         * responses, allowing you to verify the behavior of your controllers in
         * isolation.
         * -> Need to be autowired to be autoconfigured
         */
        @Autowired
        private MockMvc mockMvc;

        /* ObjectMapper provide functionalities for read and write JSON data's */
        @Autowired
        private ObjectMapper objectMapper;

        /* DEPENDENCIES MOCKING */
        /* ============================================================ */
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

        /* REFERENCES FOR MOCKING */
        /* ============================================================ */
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        User userRef = User.builder()
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .password(REFERENCE_USER_PASSWORD)
                        .role(roleForTest)
                        .build();

        CommentRequestDto commentDtoRequestRef = new CommentRequestDto(
                        "Application should provide most functionalities");

        Comment commentRef = Comment.builder()
                        .id(1)
                        .message(this.commentDtoRequestRef.msg())
                        .user(userRef)
                        .build();

        /* FIXTURES */
        /* ============================================================ */
        // @BeforeEach
        // void init() {}

        /* CONTROLLER UNIT TESTS */
        /* ============================================================ */
        @Test
        @DisplayName("Create a comment with message not empty")
        void create_shouldSuccess_whenMessageIsNotEmpty() throws Exception {
                /* Arrange */
                when(this.commentService.createComment(anyString())).thenReturn(this.commentRef);

                /* Act */
                // Send request, print response, check returned status and content type
                ResultActions response = this.mockMvc.perform(post(Endpoint.CREATE_COMMENT)
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .content(this.objectMapper.writeValueAsString(this.commentDtoRequestRef)))
                                .andDo(print())
                                .andExpect(MockMvcResultMatchers.status().isCreated())
                                .andExpect(MockMvcResultMatchers.content()
                                                .contentType(MediaType.APPLICATION_JSON_VALUE));

                // Get serialized results
                MvcResult result = response.andReturn();
                String contentAsString = result.getResponse().getContentAsString();

                // Deserialization results
                CommentResponseDto returnedComment = this.objectMapper.readValue(contentAsString,
                                CommentResponseDto.class);

                /* Assert */
                assertThat(returnedComment.message()).isEqualTo(this.commentRef.getMessage());

                /* Mocking invocation check */
                verify(this.commentService, times(1)).createComment(anyString());
        }

        @Test
        @DisplayName("Create a comment with message empty or blank")
        void create_shouldFail_whenMessageIsEmptyOrBlank() throws Exception {
                /* Arrange */
                when(this.commentService.createComment(anyString())).thenThrow(InoteEmptyMessageCommentException.class);

                /* Act & assert */
                CommentRequestDto commentDto_Request_empty = new CommentRequestDto("");
                CommentRequestDto commentDto_Request_blank = new CommentRequestDto("      ");
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

                /* Mocking invocation check */
                verify(this.commentService, times(2)).createComment(anyString());
        }

        @Test
        @DisplayName("Get all comments registered in database")
        void getComments_ShouldSuccess() throws Exception {

                /* Arrange */
                List<CommentResponseDto> comments = new ArrayList<>();
                comments.add(new CommentResponseDto(1,"this application is really crap",1));
                comments.add(new CommentResponseDto(1,"What in God's name have I done to use such an application?",1));
                comments.add(new CommentResponseDto(1,"I'm puzzled by this application...",1));
                when(this.commentService.getAll()).thenReturn(comments);

                /* Act & assert */
                this.mockMvc.perform(get(Endpoint.COMMENT_GET_ALL))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.content()
                                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                                .andExpect(MockMvcResultMatchers.content()
                                                .string(this.objectMapper.writeValueAsString(comments)));
        }
}
