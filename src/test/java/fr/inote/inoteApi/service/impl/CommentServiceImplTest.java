package fr.inote.inoteApi.service.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteEmptyMessageCommentException;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.dto.CommentDto;
import fr.inote.inoteApi.entity.Comment;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.CommentRepository;
import fr.inote.inoteApi.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {
    @Mock
    private CommentRepository commentRepository;
    @InjectMocks
    private CommentService commentService = new CommentServiceImpl(commentRepository);

    private Jwt jwtRef;
    private User userRef;
    private RefreshToken refreshToken;
    private Comment commentRef;

    private CommentDto commentDtoRef;

    @BeforeEach
    void setUp() {

        // Reference creation
        Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        this.userRef = User.builder()
                .email(REFERENCE_USER_EMAIL)
                .name(REFERENCE_USER_NAME)
                .password(REFERENCE_USER_PASSWORD)
                .role(roleForTest)
                .build();

        this.refreshToken = RefreshToken.builder()
                .expirationStatus(false)
                .contentValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiU2FuZ29rdSIsImV4cCI6MTg2OTY3NTk5Niwic3ViIjoic2FuZ29rdUBpbm90ZS5mciJ9.ni8Z4Wiyo6-noIme2ydnP1vHl6joC0NkfQ-lxF501vY")
                .creationDate(Instant.now())
                .expirationDate(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        jwtRef = Jwt.builder()
                .id(1)
                .user(this.userRef)
                .contentValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
                .deactivated(false)
                .expired(false)
                .refreshToken(this.refreshToken)
                .build();

        this.commentDtoRef = new CommentDto("Application should provide most functionalities");

        this.commentRef = Comment.builder()
                .message(this.commentDtoRef.msg())
                .build();
    }

    @Test
    @DisplayName("Create comment when user is connected")
    void createComment_ShouldSuccess_whenUserIsConnected() throws InoteEmptyMessageCommentException {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(this.userRef);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
        when(this.commentRepository.save(any(Comment.class))).thenReturn(this.commentRef);

        // Act & assert
        Comment commentForTest = this.commentService.createComment(this.commentDtoRef.msg());
//        assertThat(commentForTest.getUser()).isEqualTo(this.userRef);
        //Voir Ici! Pourquoi il ne retourne pas le user lors de la sauvegarde?
        fail();
        assertThat(commentForTest.getMessage()).isEqualTo(this.commentDtoRef.msg());

    }

    @Test
    @DisplayName("Create comment when user is not connected")
    void createComment_ShouldFail_whenUserIsNotConnected() {
        // Act & assert
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> this.commentService.createComment(this.commentDtoRef.msg()));
    }

    @Test
    @DisplayName("Create comment when user is not connected message is empty or blank")
    void createComment_ShouldFail_whenUserIsConnectedAndMessageIsEmptyOrBlank() throws InoteEmptyMessageCommentException {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(this.userRef);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Act & assert
        String commentDto1 = new String("");
        assertThatExceptionOfType(InoteEmptyMessageCommentException.class)
                .isThrownBy(() -> this.commentService.createComment(commentDto1));

        String commentDto2 = new String(" ");
        assertThatExceptionOfType(InoteEmptyMessageCommentException.class)
                .isThrownBy(() -> this.commentService.createComment(commentDto2));

        CommentDto commentDto3 = new CommentDto(null);
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> this.commentService.createComment(commentDto3.msg()));
    }


}