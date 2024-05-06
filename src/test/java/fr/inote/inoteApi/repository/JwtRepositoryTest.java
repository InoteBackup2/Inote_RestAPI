package fr.inote.inoteApi.repository;

import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static fr.inote.inoteApi.ConstantsForTests.*;

/**
 * Unit tests of repository JwtRepository
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * @DataJpaTest is an annotation in Spring Boot that is used to test JPA
 * repositories.
 * It focuses only on JPA components and disables full auto-configuration,
 * applying
 * only the configuration relevant to JPA tests.
 */
@DataJpaTest
/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes.
 * Nota : here used for using another database ok main app
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
class JwtRepositoryTest {

        /* DEPENDENCIES MOCKING */
        /* ============================================================ */
        /* use @Mock create and inject mocked instances of classes */
        @Mock
        RoleRepository mockedRoleRepository;

        /* DEPENDENCIES INJECTION */
        /* ============================================================ */
        /*
         * Use classical injection by constructor
         */
        RoleRepository roleRepository;
        UserRepository userRepository;
        JwtRepository jwtRepository;

        // Constructor
        @Autowired
        public JwtRepositoryTest(
                        RoleRepository roleRepository,
                        RoleRepository mockedRoleRepository,
                        UserRepository userRepository,
                        JwtRepository jwtRepository) {
                this.roleRepository = roleRepository;
                this.mockedRoleRepository = mockedRoleRepository;
                this.userRepository = userRepository;
                this.jwtRepository = jwtRepository;
        }

        /* REFERENCES FOR MOCKING */
        /* ============================================================ */
        private Role roleForTest = Role.builder().name(RoleEnum.ADMIN).build();
        private Jwt jwtRef;
        private RefreshToken refreshTokenRef;
        private User userRef = User.builder()
                        .email(REFERENCE_USER_EMAIL)
                        .name(REFERENCE_USER_NAME)
                        .password(REFERENCE_USER_PASSWORD)
                        .role(roleForTest)
                        .build();

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void setUp() {

                this.roleRepository.save(roleForTest);

                assertThat(this.userRepository.save(this.userRef)).isEqualTo(this.userRef);

                /*
                 * Les Claims (réclamations) dans les jetons Web JSON (JWT) sont des
                 * déclarations concernant
                 * une entité (généralement, l'utilisateur) et des données supplémentaires.
                 * Ils sont utilisés pour transmettre des informations entre les parties de
                 * manière sécurisée.
                 * Il existe trois types de claims :
                 *
                 * -> Les réclamations enregistrées : Il s'agit d'un ensemble de revendications
                 * prédéfinies qui ne sont
                 * pas obligatoires mais recommandées, afin de fournir un ensemble de
                 * revendications utiles et
                 * interopérables. En voici quelques-unes :
                 * - issuer (émetteur) : Le principal qui a émis le JWT.
                 * - exp (expiration time) : L'heure d'expiration du jeton, exprimée en heure
                 * Unix.
                 * - sub (subject) : Le sujet du JWT (typiquement, l'utilisateur).
                 * - aud (audience) : Le destinataire prévu du JWT.
                 * - iat (issued at) : heure à laquelle le JWT a été émis : L'heure à laquelle
                 * le JWT a été émis, exprimée en heure Unix.
                 *
                 * -> Réclamations publiques : Il s'agit de revendications personnalisées
                 * définies par l'application
                 * ou le développeur. Elles doivent être définies dans le registre de jetons Web
                 * JSON de l'IANA
                 * ou être définies en tant qu'URI contenant un espace de noms résistant aux
                 * collisions.
                 *
                 * -> Revendications privées : Il s'agit de revendications personnalisées
                 * utilisées par
                 * l'application ou le développeur. Elles ne sont pas enregistrées et ne doivent
                 * pas être
                 * utilisées en dehors du contexte de l'application.
                 *
                 * Les revendications se trouvent dans la section payload du JWT, qui est la
                 * deuxième partie du jeton.
                 * Voici un exemple de JWT codé en HS256 dont la charge utile contient des
                 * revendications :
                 * {
                 * "alg" : "HS256",
                 * "typ" : "JWT"
                 * }
                 * {
                 * "sub" : "1234567890",
                 * "name" : "John Doe",
                 * "admin" : true,
                 * "iat" : 1516239022
                 * }
                 * => Ce JWT indique un utilisateur dont l'id est 1234567890, nommé John Doe,
                 * qui est admin et dont
                 * la date de délivrance est le <dateUnix>.
                 * N'oubliez pas que les revendications ne doivent contenir que les informations
                 * nécessaires et pertinentes pour les besoins de l'application. Il est
                 * recommandé d'éviter d'inclure des données sensibles dans les revendications,
                 * car les JWT sont généralement transmis via HTTP et pourraient être exposés
                 * dans les communications réseau.
                 */
                final long currentTime = System.currentTimeMillis();
                final long expirationTime = currentTime + 60 * 1000; // 1mn validity

                final Map<String, Object> claims = Map.of(
                                "nom", this.userRef.getName(),
                                Claims.EXPIRATION, new Date(expirationTime),
                                Claims.SUBJECT, this.userRef.getEmail());

                /*
                 * Generation of HMAC-SH1key
                 * UN HMAC (keyed-hash message authentication code) est un type de code
                 * d'authentification de message,
                 * calculé en utilisant une fonction de hachage cryptographique en combinaison
                 * avec un clé secrète.
                 * Un code d'authentification de message est un code accompagnant des données
                 * dans le but d'assurer l'intégrité
                 * de ces dernières, en permettant de vérifier qu'elles n'ont pas subi aucune
                 * modification, après une
                 * transmission de données par exemple.
                 * Le concept est relativement semblable aux fonctions de hachage.
                 * Il s’agit ici aussi d’algorithmes qui créent un petit bloc authentificateur
                 * de taille fixe.
                 * La grande différence est que ce bloc authentificateur ne se base plus
                 * uniquement sur le message,
                 * mais également sur une clé secrète.
                 * La clé garantit que seule une personne possédant la clé secrète
                 * peut générer la valeur de hachage correcte pour un message donné.
                 */
                /*
                 * A Base64 string is a binary-to-text encoding method that represents
                 * binary data in an ASCII string format. It is commonly used to transmit
                 * data over media that only supports text, such as email.
                 * For example there : encoding of string
                 * "There is a hero if you look inside your eyes" give
                 * "VGhlcmUgaXMgYSBoZXJvIGlmIHlvdSBsb29rIGluc2lkZSB5b3VyIGV5ZXM"
                 */
                String ENCRYPTION_KEY = "VGhlcmUgaXMgYSBoZXJvIGlmIHlvdSBsb29rIGluc2lkZSB5b3VyIGV5ZXM";
                final byte[] decoder = Decoders.BASE64.decode(ENCRYPTION_KEY); // decoding Base64 secret key
                SecretKey secretKey = Keys.hmacShaKeyFor(decoder); // Encoding of HMAC

                // Building of token value
                final String jwtValue = Jwts.builder()
                                .setIssuedAt(new Date(currentTime))
                                .setExpiration(new Date(expirationTime))// expire in 1 mn
                                .setSubject(this.userRef.getEmail())
                                .setClaims(claims).signWith(secretKey, SignatureAlgorithm.HS256)
                                .compact();

                this.refreshTokenRef = RefreshToken.builder()
                                .expirationStatus(false)
                                .contentValue(jwtValue)
                                .creationDate(Instant.now())
                                .expirationDate(Instant.now().plus(60, ChronoUnit.MINUTES))
                                .build();

                this.jwtRef = Jwt.builder()
                                .contentValue(jwtValue)
                                .expired(false)
                                .deactivated(false)
                                .refreshToken(this.refreshTokenRef)
                                .user(this.userRef)
                                .build();

                assertThat(this.jwtRepository.save(this.jwtRef)).isEqualTo(this.jwtRef);

        }

        /* REPOSITORY UNIT TESTS */
        /* ============================================================ */
        @Test
        @DisplayName("Searching jwt in db by content value, deactivated and expired, with good parameters")
        void findByContentValueAndDeactivatedAndExpired_shouldReturnOptional_whenParamsAreCorrect() {
                /* Arrange */
                Optional<Jwt> result = this.jwtRepository
                                .findByContentValueAndDeactivatedAndExpired(
                                                this.jwtRef.getContentValue(),
                                                this.jwtRef.isDeactivated(),
                                                this.jwtRef.isExpired());

                /* Assert */
                assertThat(result).isNotEmpty();
                assertThat(result.get()).isEqualTo(this.jwtRef);
        }

        @Test
        @DisplayName("Searching jwt in db by content value, deactivated and expired with bad parameters")
        void findByContentValueAndDeactivatedAndExpired_shouldReturnEmptyOptional_whenAtLeastOneParameterIsBad() {
                /* Act & assert */
                Optional<Jwt> result = this.jwtRepository
                                .findByContentValueAndDeactivatedAndExpired(
                                                "Je suis une valeur très méchante",
                                                this.jwtRef.isDeactivated(),
                                                this.jwtRef.isExpired());
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findByContentValueAndDeactivatedAndExpired(
                                                this.jwtRef.getContentValue(),
                                                true,
                                                this.jwtRef.isExpired());
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findByContentValueAndDeactivatedAndExpired(
                                                this.jwtRef.getContentValue(),
                                                this.jwtRef.isDeactivated(),
                                                true);
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findByContentValueAndDeactivatedAndExpired(
                                                "Valeur qui ne s'améliore pas avec le temps",
                                                true,
                                                true);
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Searching token in db from user email and status validity with good params")
        void findTokenWithEmailAndStatusToken_shouldReturnOptional_withCorrectParams() {
                /* Act */
                Optional<Jwt> result = this.jwtRepository.findTokenWithEmailAndStatusToken(this.userRef.getEmail(),
                                false, false);

                /* Assert */
                assertThat(result).isNotEmpty();
                assertThat(result.get()).isEqualTo(this.jwtRef);
        }

        @Test
        @DisplayName("Searching token in db from user email and status validity with bad params")
        void findTokenWithEmailAndStatusToken_shouldReturnEmptyOptional_withBadParams() {
                /* Act & assert */
                Optional<Jwt> result = this.jwtRepository
                                .findTokenWithEmailAndStatusToken("yahourt@petit.suisse",
                                                false, false);
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findTokenWithEmailAndStatusToken(this.userRef.getEmail(),
                                                true, false);
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findTokenWithEmailAndStatusToken(this.userRef.getEmail(),
                                                false, true);
                assertThat(result).isEmpty();

                result = this.jwtRepository
                                .findTokenWithEmailAndStatusToken("remi-sansNomDeFamille@dass.fr",
                                                true, true);
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Retrieve Jwt in database associated with an existing email")
        void findJwtWithUserEmail_shouldReturnJwtStream_withExistingEmail() {

                /* Act & assert */
                Stream<Jwt> results = this.jwtRepository.findJwtWithUserEmail(this.userRef.getEmail());
                assertThat(results).isNotNull();
                assertThat(results).isInstanceOf(Stream.class);
                Stream<ObjectAssert<User>> objectAssertStream = results
                                .map(i -> assertThat(i.getUser()).isEqualTo(this.userRef));
                assertThat(objectAssertStream).isNotNull();
        }

        @Test
        @DisplayName("Retrieve Jwt in database associated with a bad email")
        void findJwtWithUserEmail_shouldReturnEmptyArray_withFalseEmail() {
                /* Act */
                Stream<Jwt> results = this.jwtRepository.findJwtWithUserEmail("poum-poum@tchac.com");

                /* Assert */
                assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Retrieve Jwt in db from a content value of existing refresh-token")
        void findJwtWithRefreshTokenValue_shouldReturnOptional_withExistingRefreshToken() {
                /* Act */
                Optional<Jwt> result = this.jwtRepository
                                .findJwtWithRefreshTokenValue(this.refreshTokenRef.getContentValue());

                /* Assert */
                assertThat(result).isNotEmpty();
                assertThat(result.get()).isEqualTo(this.jwtRef);
        }

        @Test
        @DisplayName("Retrieve Jwt in db from a bad content value refresh token")
        void findJwtWithRefreshTokenValue_shouldReturnEmpty_withBadRefreshTokenValue() {
                Optional<Jwt> result = this.jwtRepository.findJwtWithRefreshTokenValue("bullshitValue");
                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Delete tokens in database with matching expired and desactived status")
        void deleteAllByExpiredAndDeactivated_shouldSuccess_whenAskedParamsAreMatching() {
                /* Act */
                assertThat(this.jwtRepository.findAll()).isNotEmpty();
                this.jwtRepository.deleteAllByExpiredAndDeactivated(this.jwtRef.isExpired(),
                                this.jwtRef.isDeactivated());

                /* Assert */
                assertThat(this.jwtRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Delete token in database with non-matching expired and desactived status")
        void deleteAllByExpiredAndDeactivated_shouldFail_whenAskedParamsAreNotMatching() {

                /* Act & assert */
                assertThat(this.jwtRepository.findAll()).isNotEmpty();

                this.jwtRepository.deleteAllByExpiredAndDeactivated(!this.jwtRef.isExpired(),
                                this.jwtRef.isDeactivated());
                assertThat(this.jwtRepository.findAll()).isNotEmpty();

                this.jwtRepository.deleteAllByExpiredAndDeactivated(this.jwtRef.isExpired(),
                                !this.jwtRef.isDeactivated());
                assertThat(this.jwtRepository.findAll()).isNotEmpty();

                this.jwtRepository.deleteAllByExpiredAndDeactivated(!this.jwtRef.isExpired(),
                                !this.jwtRef.isDeactivated());
                assertThat(this.jwtRepository.findAll()).isNotEmpty();
        }
}