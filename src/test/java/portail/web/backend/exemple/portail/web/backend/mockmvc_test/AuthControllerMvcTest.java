package portail.web.backend.exemple.portail.web.backend.mockmvc_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.auth.AuthController;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.security.JwtService;
import portail.web.backend.exemple.portail.web.backend.user.UserService;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserRequest;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerMvcTest {

    @Mock UserService userService;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        AuthController controller = new AuthController(
                userService,
                authenticationManager,
                jwtService,
                userDetailsService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /api/auth/register ───────────────────────────────────────────────

    @Test
    void register_usernameDisponible_retourne201() throws Exception {
        when(userService.create(any(UserRequest.class)))
                .thenReturn(new UserResponse(1L, "alice", "ROLE_USER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "secret123"}
                                """))
                .andExpect(status().isCreated());

        verify(userService).create(any(UserRequest.class));
    }

    @Test
    void register_usernamePris_retourne400() throws Exception {
        when(userService.create(any(UserRequest.class)))
                .thenThrow(new BadRequestException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "secret123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @Test
    void login_identifiantsValides_retourne200AvecTokens() throws Exception {
        UserDetails ud = new org.springframework.security.core.userdetails.User(
                "alice", "encoded", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(ud);
        when(jwtService.generateAccessToken(ud)).thenReturn("access-jwt");
        when(jwtService.generateRefreshToken(ud)).thenReturn("refresh-jwt");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-jwt"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void login_identifiantsInvalides_retourne401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "alice", "password": "mauvais"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void me_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_avecAuthentification_retourneUsernameEtRoles() throws Exception {
        // En mode standalone sans filtre de sécurité, on injecte l'Authentication
        // via setUserPrincipal() — Authentication étend Principal, donc le resolver
        // standard de Spring MVC l'injecte dans le paramètre Authentication du controller.
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me")
                        .with(req -> { req.setUserPrincipal(auth); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.authorities").isArray());
    }
}
