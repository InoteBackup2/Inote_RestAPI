package fr.inote.inoteApi.crossCutting.security.impl;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.exceptions.InoteExpiredRefreshTokenException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteJwtNotFoundException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.repository.UserRepository;
import fr.inote.inoteApi.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static fr.inote.inoteApi.ConstantsForTests.*;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.BEARER;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;
import static fr.inote.inoteApi.crossCutting.security.JwtService.VALIDITY_TOKEN_TIME_IN_MINUTES;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests of service JwtService
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
class JwtServiceImplTest {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * @InjectMocks instance class to be tested and automatically inject mock fields
     */
    @InjectMocks
    private JwtServiceImpl jwtService;

    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* @Mock create and inject mocked instances of classes */
    @Mock
    private JwtRepository jwtRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    SecurityContextHolder securityContextHolder;

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */
    private Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
    private User userRef = User.builder()
            .email(REFERENCE_USER_EMAIL)
            .name(REFERENCE_USER_NAME)
            .password(REFERENCE_USER_PASSWORD)
            .role(roleForTest)
            .build();

    private RefreshToken refreshTokenRef = RefreshToken.builder()
            .expirationStatus(false)
            .contentValue(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiU2FuZ29rdSIsImV4cCI6MTg2OTY3NTk5Niwic3ViIjoic2FuZ29rdUBpbm90ZS5mciJ9.ni8Z4Wiyo6-noIme2ydnP1vHl6joC0NkfQ-lxF501vY")
            .creationDate(Instant.now())
            .expirationDate(Instant.now().plus(10, ChronoUnit.MINUTES))
            .build();

    private Jwt jwtRef = Jwt.builder().id(1).user(this.userRef).contentValue(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            .deactivated(false).expired(false).refreshToken(this.refreshTokenRef).build();

    /*
     * Generated token on jwt.io that contains this payload:
     * {
     * "alg": "HS256"
     * "typ": "JWT"
     * }
     * {
     * "name": "Sangoku",
     * "exp": 1869675996,
     * "sub": "sangoku@inote.fr"
     * }
     */
    final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiU2FuZ29rdSIsImV4cCI6MTg2OTY3NTk5Niwic3ViIjoic2FuZ29rdUBpbm90ZS5mciJ9.ni8Z4Wiyo6-noGme2ydnP1vHl6joC0NkfQ-lxF501vY";

    /* FIXTURES */
    /* ============================================================ */
    // @BeforeEach
    // void setUp() {}

    /* SERVICE UNIT TESTS */
    /* ============================================================ */
    @Test
    @DisplayName("Search a valid token in db with existing token")
    void findValidToken_shouldSuccess_whenValueIsPresentAndDeactivatedExpiredStatusAreFalse()
            throws InoteUserException {

        /* Arrange */
        when(this.jwtRepository.findByContentValueAndDeactivatedAndExpired(this.jwtRef.getContentValue(), false, false))
                .thenReturn(Optional.of(this.jwtRef));

        /* Act */
        Jwt validToken = this.jwtService.findValidToken(this.jwtRef.getContentValue());

        /* Assert */
        assertThat(validToken).isNotNull();
        assertThat(validToken).isEqualTo(this.jwtRef);

        /* Verify */
        verify(this.jwtRepository, times(1)).findByContentValueAndDeactivatedAndExpired(any(String.class), anyBoolean(),
                anyBoolean());

    }

    @Test
    @DisplayName("Search a valid token in db with inexistant or malformed token")
    void findValidToken_shouldFail_whenValueIsNotPresent() {
        /* Arrange */
        this.jwtRef.setContentValue("UglyToken");
        when(this.jwtRepository.findByContentValueAndDeactivatedAndExpired(this.jwtRef.getContentValue(), false, false))
                .thenReturn(Optional.empty());

        /* Act & assert */
        assertThatExceptionOfType(InoteUserException.class)
                .isThrownBy(() -> {
                    this.jwtService.findValidToken(this.jwtRef.getContentValue());
                });

        /* Verify */
        verify(this.jwtRepository, times(1)).findByContentValueAndDeactivatedAndExpired(any(String.class), anyBoolean(),
                anyBoolean());

    }

    @Test
    @DisplayName("HMAC-SHA Key generation")
    void getKey_shouldSuccess() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        /* Arrange */

        // Access to the private method using reflection
        Method privateMethod_getKey = JwtServiceImpl.class.getDeclaredMethod("getKey");
        privateMethod_getKey.setAccessible(true);

        /* Act */
        Key key = (Key) privateMethod_getKey.invoke(this.jwtService);

        /* Assert */
        assertThat(key).isNotNull();
        assertThat(key).isInstanceOf(Key.class);
    }

    @Test
    @DisplayName("Get all claims from a Jwt when token is well-formed")
    void getAllClaims_shouldSuccess_whenTokenIsWellFormed()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        /* Arrange */
        Method privateMethod_getAllClaims = JwtServiceImpl.class.getDeclaredMethod("getAllClaims", String.class);
        privateMethod_getAllClaims.setAccessible(true);

        /* Act */
        Claims claimsInJwt = (Claims) privateMethod_getAllClaims.invoke(this.jwtService, TOKEN);
        assertThat(claimsInJwt).isNotNull();

        /* Assert */
        Instant instantExpiration = claimsInJwt.getExpiration().toInstant();
        Instant instantRef = Instant.ofEpochSecond(1869675996);
        assertThat(instantExpiration).isEqualTo(instantRef);
        assertThat(claimsInJwt.getSubject()).isEqualTo(REFERENCE_USER_EMAIL);
        assertThat(claimsInJwt.get("name")).isEqualTo(REFERENCE_USER_NAME);
    }

    @Test
    @DisplayName("Get all claims from a Jwt when token is bad-formed")
    void getAllClaims_shouldFail_whenTokenIsMalFormed() throws NoSuchMethodException {

        /* Arrange */
        Method privateMethod_getAllClaims = JwtServiceImpl.class.getDeclaredMethod("getAllClaims", String.class);
        privateMethod_getAllClaims.setAccessible(true);

        /* Act */
        Throwable thrown = catchThrowable(() -> {
            privateMethod_getAllClaims.invoke(this.jwtService, "78799879");
        });

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Get particular claim in jwt")
    @Disabled
    void getClaim_shouldSuccess_whenTokenIsOKAndCalledFunctionExists() {

        // How to use getDeclaredMethod with this ?
        // private <T> T getClaim(String token, Function<Claims, T> function)
    }

    @Test
    @DisplayName("Get expiration date from valid token")
    void getExpirationDateFromToken_shouldReturnCorrectDate_whenTokenIsCorrect()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        /* Arrange */
        Method privateMethod_getExpirationDateFromToken = JwtServiceImpl.class
                .getDeclaredMethod("getExpirationDateFromToken", String.class);
        privateMethod_getExpirationDateFromToken.setAccessible(true);

        /* Act */
        Date expirationDate = (Date) privateMethod_getExpirationDateFromToken.invoke(this.jwtService, TOKEN);

        /* Assert */
        assertThat(expirationDate.toInstant()).isEqualTo(Instant.ofEpochSecond(1869675996));
    }

    @Test
    @DisplayName("Get expiration date from mal formed token")
    void getExpirationDateFromToken_shouldThrowException_whenTokenIsMalFormed() throws NoSuchMethodException {
        /* Arrange */
        Method privateMethod_getExpirationDateFromToken = JwtServiceImpl.class
                .getDeclaredMethod("getExpirationDateFromToken", String.class);
        privateMethod_getExpirationDateFromToken.setAccessible(true);

        /* Act */
        Throwable thrown = catchThrowable(
                () -> privateMethod_getExpirationDateFromToken.invoke(this.jwtService, "jdsljdslflsdfl"));

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Get expiration status of token")
    void isTokenExpired_shouldSuccess_whenTokenIsWellFormed() {
        /* Act & assert */
        assertThat(this.jwtService.isTokenExpired(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("Get expiration status when token is expired")
    void isTokenExpired_shouldThrowException_whenTokenIsExpired() {
        /* Act */
        Throwable thrown = catchThrowable(() -> {
            // With expirations Date Saturday 31 March 2018 14:04:49
            this.jwtService.isTokenExpired(
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiU2FuZ29rdSIsImV4cCI6MTUyMjUwNTA4OSwic3ViIjoic2FuZ29rdUBpbm90ZS5mciJ9.xOFJfXiXgt11hNu2u_Oj6jp6PuMJTNogrHye_Sr8p_k");
        });

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Get expiration status when token is malformed")
    void isTokenExpired_shouldThrowException_whenTokenIsMalFormed() {
        /* Act */
        Throwable thrown = catchThrowable(() -> {
            // With expirations Date Saturday 31 March 2018 14:04:49
            this.jwtService.isTokenExpired("MalformedToken");
        });

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Extract username in claims when token is OK")
    void extractUserName_shouldSuccess_whenTokenIsCorrect() {
        /* Act & assert */
        assertThat(this.jwtService.extractUsername(TOKEN)).isEqualTo(REFERENCE_USER_EMAIL);
    }

    @Test
    @DisplayName("Extract username in claims when token is malformed")
    void extractUserName_shouldThrowException_whenTokenIsMalformed() {

        /* Act */
        Throwable thrown = catchThrowable(() -> {
            // With expirations Date Saturday 31 March 2018 14:04:49
            this.jwtService.extractUsername("MalformedToken");
        });

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(MalformedJwtException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate a token from user with correct user")
    void generateJwt_shouldSuccess_whenUserIsCorrect()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {

        /* Arrange */
        Map<String, String> jwtTest;

        Method privateMethod_generateToken = JwtServiceImpl.class.getDeclaredMethod("generateJwt", User.class);
        privateMethod_generateToken.setAccessible(true);

        Method privateMethod_getAllClaims = JwtServiceImpl.class.getDeclaredMethod("getAllClaims", String.class);
        privateMethod_getAllClaims.setAccessible(true);

        /* Act */
        Instant instantOfCreation = Instant.now();
        sleep(1000);
        jwtTest = (Map<String, String>) privateMethod_generateToken.invoke(this.jwtService, this.userRef);

        /* Assert */
        Claims claims = (Claims) privateMethod_getAllClaims.invoke(this.jwtService, jwtTest.get("bearer"));
        assertThat(claims.get("name")).isEqualTo(this.userRef.getName());
        assertThat(claims.getSubject()).isEqualTo(this.userRef.getEmail());
        assertThat(claims.getExpiration())
                .isAfter(instantOfCreation.plus(VALIDITY_TOKEN_TIME_IN_MINUTES, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("Generate Map containing token and refreshToken whith existing user")
    void generate_shouldReturnCorrectMap_whenUserExistInDb()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InterruptedException {
        /* Arrange */
        when(this.userService.loadUserByUsername(anyString())).thenReturn(this.userRef);
        when(this.jwtRepository.save(any(Jwt.class))).thenReturn(any(Jwt.class));

        Method privateMethod_getAllClaims = JwtServiceImpl.class.getDeclaredMethod("getAllClaims", String.class);
        privateMethod_getAllClaims.setAccessible(true);

        /* Act */
        Instant instantOfCreation = Instant.now();
        sleep(1000);
        Map<String, String> jwtMapTest = this.jwtService.generate(this.userRef.getUsername());

        /* Assert */
        String tokenValue = jwtMapTest.get("bearer");
        Claims claims = (Claims) privateMethod_getAllClaims.invoke(this.jwtService, tokenValue);
        assertThat(claims.get("name")).isEqualTo(this.userRef.getName());
        assertThat(claims.getSubject()).isEqualTo(this.userRef.getEmail());
        assertThat(claims.getExpiration())
                .isAfter(instantOfCreation.plus(VALIDITY_TOKEN_TIME_IN_MINUTES, ChronoUnit.MINUTES));

        String refreshToken = jwtMapTest.get("refresh");
        assertThat(refreshToken).isNotNull();

        /* Verify */
        verify(this.userService, times(1)).loadUserByUsername(any(String.class));
        verify(this.jwtRepository, times(1)).save(any(Jwt.class));

    }

    @Test
    @DisplayName("Generate Map containing token and refreshToken whith non existing user in db")
    void generate_shouldThrowException_whenUserNotExistsInDb() {
        /* Arrange */
        this.userRef.setEmail("loch@ness.sc");
        when(this.userService.loadUserByUsername(any(String.class))).thenThrow(UsernameNotFoundException.class);

        /* Act */
        Throwable thrown = catchThrowable(() -> {
            // With expirations Date Saturday 31 March 2018 14:04:49
            this.jwtService.generate(this.userRef.getUsername());
        });

        /* Assert */
        assertThat(thrown)
                .isInstanceOf(UsernameNotFoundException.class);

        /* Verify */
        verify(this.userService, times(1)).loadUserByUsername(any(String.class));
    }

    @Test
    @DisplayName("refresh connection with token value")
    void refreshConnectionWithRefreshTokenValue_ShouldSuccess_WhenFirstJwtIsRetrievedAndRefreshTokenIsNotExpired()
            throws InoteJwtNotFoundException, InoteExpiredRefreshTokenException {

        /* Arrange */
        when(this.jwtRepository.findJwtWithRefreshTokenValue(any(String.class))).thenReturn(Optional.of(this.jwtRef));

        Mockito.doAnswer((Answer<Stream<Jwt>>) invocation -> {
            invocation.getArgument(0);
            List<Jwt> jwts = new ArrayList<>();
            jwts.add(jwtRef);
            return jwts.stream();
        }).when(this.jwtRepository).findJwtWithUserEmail(Mockito.anyString());

        when(this.jwtRepository.save(any(Jwt.class))).thenReturn(this.jwtRef);
        when(this.userService.loadUserByUsername(any(String.class))).thenReturn(this.userRef);

        /* Act */
        Map<String, String> returnValue = this.jwtService
                .refreshConnectionWithRefreshTokenValue(this.jwtRef.getRefreshToken().getContentValue());

        /* Assert */
        assertThat(returnValue.get(BEARER)).isNotNull();
        assertThat(returnValue.get(BEARER).length()).isEqualTo(145);
        assertThat(returnValue.get(REFRESH)).isNotNull();
        assertThat(returnValue.get(REFRESH).length()).isEqualTo(UUID.randomUUID().toString().length());
    }

    @Test
    @DisplayName("refresh connection with bad token value")
    void refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenContentValueNotExistInDb()
            throws InoteJwtNotFoundException, InoteExpiredRefreshTokenException {

        /* Arrange */
        when(this.jwtRepository.findJwtWithRefreshTokenValue(any(String.class))).thenReturn(Optional.empty());

        /* Act & assert */
        assertThatExceptionOfType(InoteJwtNotFoundException.class)
                .isThrownBy(() -> this.jwtService.refreshConnectionWithRefreshTokenValue("1234"));
    }

    @Test
    @DisplayName("refresh connection with bad token value")
    void refreshConnectionWithRefreshTokenValue_ShouldFail_WhenRefreshTokenIsExpired()
            throws InoteExpiredRefreshTokenException {

        /* Arrange */
        this.jwtRef.getRefreshToken().setExpirationDate(Instant.now().minus(2, ChronoUnit.MINUTES));
        this.jwtRef.getRefreshToken().setExpirationStatus(true);
        when(this.jwtRepository.findJwtWithRefreshTokenValue(any(String.class))).thenReturn(Optional.of(this.jwtRef));

        /* Act & assert */
        assertThatExceptionOfType(InoteExpiredRefreshTokenException.class)
                .isThrownBy(() -> this.jwtService.refreshConnectionWithRefreshTokenValue("123456"));
    }

    @Test
    @DisplayName("SignOut when user is effectively connected")
    void SignOut_ShouldSuccess_whenValidTokenExists() throws InoteJwtNotFoundException {
        /* Arrange */
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(this.userRef);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(this.jwtRepository.findTokenWithEmailAndStatusToken(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.of(this.jwtRef));
        when(this.jwtRepository.save(any(Jwt.class))).thenReturn(this.jwtRef);

        /* Act & assert */
        assertThatCode(() -> this.jwtService.signOut()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SignOut when user is not connected")
    void SignOut_ShouldFail_whenValidTokenNotFound() {
        /* Arrange */
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(this.userRef);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(this.jwtRepository.findTokenWithEmailAndStatusToken(anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(Optional.empty());

        /* Act & assert */
        assertThatExceptionOfType(InoteJwtNotFoundException.class).isThrownBy(() -> this.jwtService.signOut());
    }
}