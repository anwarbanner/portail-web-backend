package portail.web.backend.exemple.portail.web.backend.junit_test;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import portail.web.backend.exemple.portail.web.backend.security.JwtAuthenticationFilter;
import portail.web.backend.exemple.portail.web.backend.security.JwtService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;
    @InjectMocks JwtAuthenticationFilter filter;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── no token ──────────────────────────────────────────────────────────────

    @Test
    void noAuthorizationHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void authHeaderWithoutBearerPrefix_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── invalid token ─────────────────────────────────────────────────────────

    @Test
    void invalidJwtToken_doesNotAuthenticate() throws Exception {
        when(jwtService.extractUsername("bad.token.here"))
                .thenThrow(new JwtException("invalid"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── valid token ───────────────────────────────────────────────────────────

    @Test
    void validToken_authenticatesAndSetsSecurityContext() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.extractUsername("valid.token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.token", userDetails)).thenReturn(true);
        when(jwtService.extractAuthorities("valid.token"))
                .thenReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void validToken_emptyAuthoritiesFromJwt_fallsBackToUserDetailsAuthorities() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.extractUsername("valid.token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.token", userDetails)).thenReturn(true);
        when(jwtService.extractAuthorities("valid.token")).thenReturn(List.of());
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(userDetails).getAuthorities();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_ADMIN");
    }

    @Test
    void validToken_invalidSignature_doesNotAuthenticate() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);

        when(jwtService.extractUsername("valid.token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.token", userDetails)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
