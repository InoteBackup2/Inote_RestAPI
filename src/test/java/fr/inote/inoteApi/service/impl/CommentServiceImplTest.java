package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.dto.CommentDtoRequest;
import fr.inote.inoteApi.dto.CommentDtoResponse;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests of service CommentService
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

        /* DEPENDENCIES MOCKING */
        /* ============================================================ */
        /* @Mock create and inject mocked instances of classes */
        @Mock
        private CommentRepository commentRepository;
        @Mock
        private UserRepository userRepository;

        /* DEPENDENCIES INJECTION */
        /* ============================================================ */
        /*
         * @InjectMocks instance class to be tested and automatically inject mock fields
         * Nota : if service is an interface, instanciate implementation withs mocks in
         * params
         */
        @InjectMocks
        private CommentService commentService = new CommentServiceImpl(commentRepository, userRepository);

        /* REFERENCES FOR MOCKING */
        /* ============================================================ */
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();

        private User userRef = User.builder()
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .password(REFERENCE_USER_PASSWORD)
                        .role(roleForTest)
                        .build();

        private CommentDtoRequest commentDtoRequestRef = new CommentDtoRequest(
                        "Application should provide most functionalities");

        private Comment commentRef = Comment.builder()
                        .message(this.commentDtoRequestRef.msg())
                        .user(this.userRef)
                        .build();

        /* FIXTURES */
        /* ============================================================ */
        // @BeforeEach
        // void setUp() {}

        /* SERVICE UNIT TESTS */
        /* ============================================================ */
        @Test
        @DisplayName("Create comment when user is connected")
        void createComment_ShouldSuccess_whenUserIsConnected() throws InoteEmptyMessageCommentException {

                /* Arrange */
                Authentication auth = mock(Authentication.class);
                when(auth.getPrincipal()).thenReturn(this.userRef);
                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(auth);
                SecurityContextHolder.setContext(securityContext);

                when(this.commentRepository.save(any(Comment.class))).thenReturn(this.commentRef);
                when(this.userRepository.findByEmail(anyString())).thenReturn(Optional.of(this.userRef));

                /* Act */
                Comment commentForTest = this.commentService.createComment(this.commentDtoRequestRef.msg());

                /* Assert */
                assertThat(commentForTest.getMessage()).isEqualTo(this.commentDtoRequestRef.msg());

        }

        @Test
        @DisplayName("Create comment when user is not connected")
        void createComment_ShouldFail_whenUserIsNotConnected() {
                /* Arrange */
                // Authentication auth = mock(Authentication.class);
                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(null);
                SecurityContextHolder.setContext(securityContext);

                /* Act & assert */
                assertThatExceptionOfType(NullPointerException.class)
                                .isThrownBy(() -> this.commentService.createComment(this.commentDtoRequestRef.msg()));
        }

        @Test
        @DisplayName("Create comment when user is not connected message is empty or blank")
        void createComment_ShouldFail_whenUserIsConnectedAndMessageIsEmptyOrBlank()
                        throws InoteEmptyMessageCommentException {

                /* Arrange */
                Authentication auth = mock(Authentication.class);
                when(auth.getPrincipal()).thenReturn(this.userRef);
                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(auth);
                SecurityContextHolder.setContext(securityContext);

                /* Act & assert */
                String commentDto1 = new String("");
                assertThatExceptionOfType(InoteEmptyMessageCommentException.class)
                                .isThrownBy(() -> this.commentService.createComment(commentDto1));

                String commentDto2 = new String(" ");
                assertThatExceptionOfType(InoteEmptyMessageCommentException.class)
                                .isThrownBy(() -> this.commentService.createComment(commentDto2));

                CommentDtoRequest commentDtoRequest3 = new CommentDtoRequest(null);
                assertThatExceptionOfType(NullPointerException.class)
                                .isThrownBy(() -> this.commentService.createComment(commentDtoRequest3.msg()));
        }

        @Test
        @DisplayName("Get the list of all comments recorded in database")
        void getAll_ShouldSucces() {

                /* Arrange */
                List<Comment> comments = new ArrayList<>();

                comments.add(Comment.builder()
                                .message("this application is really crap")
                                .user(this.userRef)
                                .build());

                comments.add(Comment.builder()
                                .message("What in God's name have I done to use such an application?")
                                .user(this.userRef)
                                .build());

                comments.add(Comment.builder()
                                .message("I'm puzzled by this application...")
                                .user(this.userRef)
                                .build());
                when(this.commentRepository.findAll()).thenReturn(comments);

                /*Act */
                List<CommentDtoResponse> returnedList = this.commentService.getAll();

                /*Assert */
                assertThat(returnedList.get(0).message()).isEqualTo(comments.get(0).getMessage());
                assertThat(returnedList.get(1).message()).isEqualTo(comments.get(1).getMessage());
                assertThat(returnedList.get(2).message()).isEqualTo(comments.get(2).getMessage());

                /*Verify */
                verify(this.commentRepository, times(1)).findAll();

        }

}