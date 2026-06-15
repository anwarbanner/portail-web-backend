package portail.web.backend.exemple.portail.web.backend.mockmvc_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.abonnement.controller.AbonnementController;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.MonAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementService;
import portail.web.backend.exemple.portail.web.backend.consultation.service.ConsultationService;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AbonnementControllerMvcTest {

    @Mock AbonnementService abonnementService;
    @Mock ConsultationService consultationService;
    @Mock UserService userService;

    @InjectMocks AbonnementController abonnementController;

    private MockMvc mockMvc;

    @JsonIgnoreProperties({"pageable", "sort"})
    abstract static class PageImplMixin {}

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setup() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(PageImpl.class, PageImplMixin.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(abonnementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AbonnementResponse abonnementResponse(Long id) {
        return new AbonnementResponse(
                id, 10L, "alice", 1L, "Premium",
                LocalDate.now(), LocalDate.now().plusMonths(12),
                "ACTIVE", null, true, true, LocalDateTime.now());
    }

    private MonAbonnementResponse monAbonnementResponse() {
        return new MonAbonnementResponse(
                1L, "Premium", "Description", new BigDecimal("99.00"),
                12, null, LocalDate.now(), LocalDate.now().plusMonths(12),
                "ACTIVE", null, true, true);
    }

    // En standalone MockMvc, Authentication étend Principal →
    // setUserPrincipal() permet au resolver standard d'injecter l'Authentication.
    private RequestPostProcessor withAuth(String username, String role) {
        return req -> {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            req.setUserPrincipal(auth);
            return req;
        };
    }

    // ── GET /api/admin/abonnements ────────────────────────────────────────────

    @Test
    void listAbonnements_retourne200AvecPage() throws Exception {
        when(abonnementService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(abonnementResponse(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/abonnements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    // ── GET /api/admin/abonnements/{id} ──────────────────────────────────────

    @Test
    void getAbonnementById_abonnementExistant_retourne200() throws Exception {
        when(abonnementService.findById(1L)).thenReturn(abonnementResponse(1L));

        mockMvc.perform(get("/api/admin/abonnements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIVE"));
    }

    @Test
    void getAbonnementById_abonnementInexistant_retourne404() throws Exception {
        when(abonnementService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Abonnement not found with id=99"));

        mockMvc.perform(get("/api/admin/abonnements/99"))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/admin/abonnements ───────────────────────────────────────────

    @Test
    void creerAbonnement_donneesValides_retourne201() throws Exception {
        when(abonnementService.creer(any())).thenReturn(abonnementResponse(1L));

        mockMvc.perform(post("/api/admin/abonnements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 10, "planId": 1, "dateDebut": "2026-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"));
    }

    @Test
    void creerAbonnement_champObligatoireManquant_retourne400() throws Exception {
        mockMvc.perform(post("/api/admin/abonnements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId": 1, "dateDebut": "2026-01-01"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/admin/abonnements/{id} ──────────────────────────────────────

    @Test
    void modifierAbonnement_donneesValides_retourne200() throws Exception {
        when(abonnementService.modifier(eq(1L), any())).thenReturn(abonnementResponse(1L));

        mockMvc.perform(put("/api/admin/abonnements/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateFin": "2027-01-01", "statut": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ── DELETE /api/admin/abonnements/{id} ────────────────────────────────────

    @Test
    void annulerAbonnement_abonnementExistant_retourne204() throws Exception {
        doNothing().when(abonnementService).annuler(1L);

        mockMvc.perform(delete("/api/admin/abonnements/1"))
                .andExpect(status().isNoContent());

        verify(abonnementService).annuler(1L);
    }

    // ── GET /api/abonnements/me ───────────────────────────────────────────────

    @Test
    void monAbonnement_avecAbonnementActif_retourne200() throws Exception {
        when(userService.findIdByUsername("alice")).thenReturn(10L);
        when(abonnementService.findMonActif(10L)).thenReturn(Optional.of(monAbonnementResponse()));

        mockMvc.perform(get("/api/abonnements/me").with(withAuth("alice", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planNom").value("Premium"))
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    void monAbonnement_sansAbonnementActif_retourne204() throws Exception {
        when(userService.findIdByUsername("alice")).thenReturn(10L);
        when(abonnementService.findMonActif(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/abonnements/me").with(withAuth("alice", "USER")))
                .andExpect(status().isNoContent());
    }

    // ── GET /api/abonnements/me/consultations ─────────────────────────────────

    @Test
    void mesConsultations_retourne200AvecPage() throws Exception {
        when(userService.findIdByUsername("alice")).thenReturn(10L);
        when(consultationService.findByUser(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/abonnements/me/consultations").with(withAuth("alice", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
