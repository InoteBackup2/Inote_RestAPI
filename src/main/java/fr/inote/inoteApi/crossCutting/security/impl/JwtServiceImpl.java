package fr.inote.inoteApi.crossCutting.security.impl;

import fr.inote.inoteApi.crossCutting.constants.HttpRequestBody;
import fr.inote.inoteApi.crossCutting.exceptions.InoteExpiredRefreshTokenException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteJwtNotFoundException;
import fr.inote.inoteApi.crossCutting.exceptions.InoteUserException;
import fr.inote.inoteApi.crossCutting.security.Jwt;
import fr.inote.inoteApi.crossCutting.security.RefreshToken;
import fr.inote.inoteApi.crossCutting.security.JwtService;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.repository.JwtRepository;
import fr.inote.inoteApi.service.UserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.BEARER;
import static fr.inote.inoteApi.crossCutting.constants.HttpRequestBody.REFRESH;

@Slf4j
@Transactional // transactional behavior.
@Service
public class JwtServiceImpl implements JwtService {

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    private UserService userService;
    private JwtRepository jwtRepository;

    @Autowired
    public JwtServiceImpl(
            UserService userService,
            JwtRepository jwtRepository) {
        this.userService = userService;
        this.jwtRepository = jwtRepository;
    }

    /* CONSTANTS */
    /* ============================================================ */
    // Reference for encryption

    // @Value("${jwt.encryptionKey}")
    private final String ENCRYPTION_KEY = "7b751e0c0ca1e5f099bf9cca5a8217ee263bb8679303ddcc3cbfbe13ab79156e";

    /* PUBLIC METHODS */
    /* ============================================================ */

    /**
     * Retrieve a token in database, actived and not expired
     *
     * @param value value of token to search in database
     * @return the JWT
     */
    public Jwt findValidToken(String value) throws InoteUserException {
        return this.jwtRepository.findByContentValueAndDeactivatedAndExpired(
                value,
                false,
                false).orElseThrow(() -> new InoteUserException());
    }

    /**
     * Generate a token and refresh token from username place it in a Map and
     * returns
     *
     * @param username to assign token
     * @return Map containing token and refresh token
     */
    public Map<String, String> generate(String username) {

        User user = (User) this.userService.loadUserByUsername(username);

        // Desactivation of all actual tokens of user
        // They will be removed by schleduled task
        this.disableTokens(user);

        // Generate token and put it in HasMap
        final Map<String, String> jwtMap = new HashMap<>(this.generateJwt(user));

        // Refresh-token creation
        RefreshToken refreshToken = RefreshToken.builder()
                .contentValue(UUID.randomUUID().toString()) // Universal Unique IDentifier
                .expirationStatus(false)
                .creationDate(Instant.now())
                .expirationDate(Instant.now().plus(VALIDITY_TOKEN_TIME_IN_MINUTES, ChronoUnit.MINUTES))
                .build();

        /* create the jwt and store in db for activation before expirationDate */
        final Jwt jwt = Jwt
                .builder()
                .contentValue(jwtMap.get(HttpRequestBody.BEARER))
                .deactivated(false)
                .expired(false)
                .user(user)
                .refreshToken(refreshToken)
                .build();
        this.jwtRepository.save(jwt);

        jwtMap.put(REFRESH, refreshToken.getContentValue());

        return jwtMap;
    }

    /**
     * Desactive tokens of an user
     * 
     * @param user
     */
    private void disableTokens(User user) {
        final List<Jwt> jwtList = this.jwtRepository.findJwtWithUserEmail(user.getEmail()).peek(
                jwt -> {
                    jwt.setDeactivated(true);
                    jwt.setExpired(true);
                }).collect(Collectors.toList());

        this.jwtRepository.saveAll(jwtList);
    }

    /**
     * Extract username from token
     *
     * @param token to be parsed
     * @return the username in token
     */
    public String extractUsername(String token) {
        return this.getClaim(token, Claims::getSubject);
    }

    /**
     * get expiration status of token
     *
     * @param token to be parsed
     * @return a boolean indicate the status
     */
    public boolean isTokenExpired(String token) {
        Date expirationDate = getExpirationDateFromToken(token);
        return expirationDate.before(new Date());
    }

    /**
     * Get expiration date of token
     *
     * @param token to be parsed
     * @return the date of expiration
     */
    private Date getExpirationDateFromToken(String token) {
        return this.getClaim(token, Claims::getExpiration);
    }

    /**
     * Get claim in token
     *
     * @param token    to be parses
     * @param function ti be call from claim
     * @return the desired claim
     */
    private <T> T getClaim(String token, Function<Claims, T> function) {
        Claims claims = getAllClaims(token);
        return function.apply(claims);
    }

    /**
     * Get all claims (datas) in token
     * <p>
     * Nota : Jwt is formed by 3 parts:
     * -> the header, contains used algorithm and type of token
     * -> the payload, containing datas
     * -> the signature : the HMAC-SHA key
     *
     * @param token the jwt to parse
     * @return a map that contains datas
     */
    private Claims getAllClaims(String token) {

        /* Creation of parser with the secret key */
        JwtParserBuilder parserBuilder = Jwts.parserBuilder()
                .setSigningKey(this.getKey());
        // Compilation of parser
        JwtParser parser = parserBuilder.build();

        /* Token analyze whith our parser, then validation */
        Jws<Claims> parsedJwt = parser.parseClaimsJws(token);

        /* Claims extraction */

        return parsedJwt.getBody();
    }

    /**
     * Generate a jwt from an user
     *
     * @param user to affect to jwt
     * @return map with key "bearer" and value the token value
     */
    private Map<String, String> generateJwt(User user) {
        final long currentTime = System.currentTimeMillis();
        final long expirationTime = currentTime + VALIDITY_TOKEN_TIME_IN_MINUTES * 60 * 1000; // 600s of validity

        final Map<String, Object> claims = Map.of(
                "name", user.getName(),
                Claims.EXPIRATION, new Date(expirationTime),
                Claims.SUBJECT, user.getEmail());

        final String bearer = Jwts.builder()
                .setIssuedAt(new Date())
                .setExpiration(new Date(expirationTime))
                .setSubject(user.getEmail())
                .setClaims(claims)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();

        return Map.of(BEARER, bearer);
    }

    /**
     * Generate an HMAC-SHA Key
     * Un code d'authentification de message (MAC) est un code accompagnant des
     * données dans le but
     * d'assurer l'intégrité de ces dernières, en permettant de vérifier qu'elles
     * n'ont subi aucune
     * modification, après une transmission par exemple.
     * Le concept est relativement semblable aux fonctions de hachage. Il s’agit ici
     * aussi
     * d’algorithmes qui créent un petit bloc authentificateur de taille fixe.
     * La grande différence est que ce bloc authentificateur ne se base plus
     * uniquement sur le message,
     * mais également sur une clé secrète.
     * Tout comme les fonctions de hachage, les MAC n’ont pas besoin d’être
     * réversibles.
     * En effet, le récepteur exécutera le même calcul sur le message et le
     * comparera avec le MAC reçu.
     * Le MAC assure non seulement une fonction de vérification de l'intégrité du
     * message, comme le
     * permettrait une simple fonction de hachage mais de plus authentifie
     * l’expéditeur, détenteur de la
     * clé secrète. Il peut également être employé comme un chiffrement
     * supplémentaire (rare) e
     * t peut être calculé avant ou après le chiffrement principal, bien qu’il soit
     * généralement conseillé
     * de le faire après (Encrypt-then-MAC, on chiffre d'abord le message, puis on
     * transmet le message
     * chiffré ainsi que son MAC).
     * Un HMAC est calculé en utilisant un algorithme cryptographique qui combine
     * une fonction de hachage
     * cryptographique (comme SHA-256 ou SHA-512) avec une clé secrète.
     * Seuls les participants à la conversation connaissent la clé secrète,
     * et le résultat de la fonction de hachage dépend à présent des données
     * d'entrée et de la clé secrète.
     * Seules les parties qui ont accès à cette clé secrète peuvent calculer
     * le condensé d'une fonction HMAC. Cela permet de vaincre les attaques de type
     * "man-in-the-middle" et d'authentifier l'origine des données. L'intégrité est
     * assurée quant à elle par les fonctions de hachage.
     *
     * @return the key
     */
    private Key getKey() {
        final byte[] decoder = Decoders.BASE64.decode(ENCRYPTION_KEY);
        return Keys.hmacShaKeyFor(decoder);
    }

    /**
     * Signout of the user
     * <p>
     * Retrieve the authenticated user and his token with his email.
     * The token is deactivated and saved in database.
     */
    public void signOut() throws InoteJwtNotFoundException {
        // Get current user
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Jwt jwt = this.jwtRepository.findTokenWithEmailAndStatusToken(
                user.getEmail(),
                false,
                false).orElseThrow(InoteJwtNotFoundException::new);
        jwt.setExpired(true);
        jwt.setDeactivated(true);
        this.jwtRepository.save(jwt);
    }

    /**
     * Schleduled suppression of all expired/desactived tokens
     * <p>
     * Use a cron expression (www.cron.guru)
     * Crontable syntax:
     * # .---------------- minute (0 - 59)
     * # | .------------- hour (0 - 23)
     * # | | .---------- day of month (1 - 31)
     * # | | | .------- month (1 - 12) OR jan,feb,mar,apr ...
     * # | | | | .---- day of week (0 - 6) (Sunday=0 or 7) OR
     * sun,mon,tue,wed,thu,fri,sat
     * # | | | | |
     * # * * * * * user command to be executed
     * exemple: for execute task every days at 04:05:
     * => @Scheduled(cron = "5 4 * * *")
     * Is possible to use annotations like @daily (voir
     * crontable.guru)
     */
    @Scheduled(cron = "0 * * * * ?") // Execution every minute
    // @Scheduled(cron = "@daily") // Execution every days at midnight
    public void removeUselessJwt() {
        log.info("Inactive/expired tokens suppression at {}", Instant.now());
        this.jwtRepository.deleteAllByExpiredAndDeactivated(true, true);
    }

    /**
     * When the jwt is expired and rejected by the server during a request for
     * access to a protected resource,
     * the HTTP client can transmit its refresh token to the server.
     * If this refresh token is valid, the server deletes all
     * tokens concerning the user,
     * generates a new token and its refresh token.
     *
     * @param tokenValue
     * @return a Map containing the refresh token
     */
    public Map<String, String> refreshConnectionWithRefreshTokenValue(String tokenValue)
            throws InoteJwtNotFoundException, InoteExpiredRefreshTokenException {

        // find the first jwt with value of refreshToken
        final Jwt jwt = this.jwtRepository.findJwtWithRefreshTokenValue(tokenValue)
                .orElseThrow(InoteJwtNotFoundException::new);

        // now, get refreshTokenStatus and check if is valid
        if (jwt.getRefreshToken().isExpirationStatus()
                || jwt.getRefreshToken().getExpirationDate().isBefore(Instant.now())) {
            throw new InoteExpiredRefreshTokenException();
        }

        // All related user tokens desactivation, include refreshToken
        this.disableTokens(jwt.getUser());

        // Generation of new token + refresh token
        return this.generate(jwt.getUser().getEmail());
    }
}
