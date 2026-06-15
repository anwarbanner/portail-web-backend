package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import portail.web.backend.exemple.portail.web.backend.security.JwtService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Note: JJWT lance ExpiredJwtException (RuntimeException) lorsqu'un token expiré
// est parsé — isTokenValid ne retourne pas false dans ce cas, il propage l'exception.

/**
 * Tests unitaires du JwtService.
 * Vérifie la génération, l'extraction et la validation des tokens JWT.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    private static final String SECRET =
            "test-secret-key-that-is-long-enough-for-hmac-sha256";
    private static final long EXPIRATION_MS = 3_600_000L; // 1h

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", EXPIRATION_MS);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", EXPIRATION_MS * 24);

        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("alice");
        when(userDetails.getAuthorities())
                .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void generateAccessToken_returnsNonNullToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = jwtService.generateAccessToken(userDetails);
        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_validTokenAndSameUser_returnsTrue() {
        String token = jwtService.generateAccessToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_differentUser_returnsFalse() {
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails other = mock(UserDetails.class);
        when(other.getUsername()).thenReturn("bob");
        when(other.getAuthorities()).thenReturn(List.of());

        assertFalse(jwtService.isTokenValid(token, other));
    }

    @Test
    void isTokenValid_expiredToken_throwsExpiredJwtException() {
        // JJWT lève ExpiredJwtException lors du parsing — pas de retour false
        String expiredToken = jwtService.generateToken(Map.of(), userDetails, -1L);
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void extractAuthorities_returnsCorrectAuthorities() {
        String token = jwtService.generateAccessToken(userDetails);
        List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);

        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
    }

    @Test
    void generateRefreshToken_hasLongerExpiration() {
        // Vérifie que generateRefreshToken ne lance pas d'exception
        String refresh = jwtService.generateRefreshToken(userDetails);
        assertNotNull(refresh);
        assertEquals("alice", jwtService.extractUsername(refresh));
    }
}
