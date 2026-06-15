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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.paiement.controller.PaiementController;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementResponse;
import portail.web.backend.exemple.portail.web.backend.paiement.service.PaiementService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests MockMvc du PaiementController — approche standalone (Spring Boot 4.x).
 * Vérifie les opérations de gestion des paiements (admin uniquement).
 */
@ExtendWith(MockitoExtension.class)
class PaiementControllerMvcTest {

    @Mock PaiementService paiementService;

    @InjectMocks PaiementController paiementController;

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
                .standaloneSetup(paiementController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PaiementResponse paiementResponse(Long id) {
        return new PaiementResponse(
                id, 1L, 10L, "alice",
                new BigDecimal("99.00"), LocalDateTime.now(),
                "VIREMENT", "REF-001", "COMPLETE", LocalDateTime.now());
    }

    // ── GET /api/admin/paiements ──────────────────────────────────────────────

    @Test
    void listPaiements_retourne200AvecPage() throws Exception {
        when(paiementService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(paiementResponse(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/paiements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.content[0].statutPaiement").value("COMPLETE"));
    }

    @Test
    void listPaiements_aucunPaiement_retourne200ListeVide() throws Exception {
        when(paiementService.findAll(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/admin/paiements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /api/admin/paiements/{id} ─────────────────────────────────────────

    @Test
    void getPaiementById_paiementExistant_retourne200() throws Exception {
        when(paiementService.findById(1L)).thenReturn(paiementResponse(1L));

        mockMvc.perform(get("/api/admin/paiements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.referenceTransaction").value("REF-001"))
                .andExpect(jsonPath("$.montant").value(99.00));
    }

    @Test
    void getPaiementById_paiementInexistant_retourne404() throws Exception {
        when(paiementService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Paiement not found with id=99"));

        mockMvc.perform(get("/api/admin/paiements/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Paiement not found with id=99"));
    }

    // ── GET /api/admin/paiements/abonnement/{id} ──────────────────────────────

    @Test
    void paiementsByAbonnement_retourne200AvecPage() throws Exception {
        when(paiementService.findByAbonnement(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(paiementResponse(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/paiements/abonnement/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].abonnementId").value(1));
    }

    @Test
    void paiementsByAbonnement_abonnementSansPaiements_retourne200ListeVide() throws Exception {
        when(paiementService.findByAbonnement(eq(99L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/admin/paiements/abonnement/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── POST /api/admin/paiements ─────────────────────────────────────────────

    @Test
    void enregistrerPaiement_donneesValides_retourne201() throws Exception {
        when(paiementService.enregistrer(any())).thenReturn(paiementResponse(1L));

        mockMvc.perform(post("/api/admin/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"abonnementId": 1, "montant": 99.00,
                                 "methodePaiement": "VIREMENT",
                                 "referenceTransaction": "REF-001",
                                 "statutPaiement": "COMPLETE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statutPaiement").value("COMPLETE"))
                .andExpect(jsonPath("$.referenceTransaction").value("REF-001"));
    }

    @Test
    void enregistrerPaiement_montantNul_retourne400() throws Exception {
        mockMvc.perform(post("/api/admin/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"abonnementId": 1,
                                 "methodePaiement": "VIREMENT",
                                 "statutPaiement": "COMPLETE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enregistrerPaiement_abonnementInexistant_retourne404() throws Exception {
        when(paiementService.enregistrer(any()))
                .thenThrow(new ResourceNotFoundException("Abonnement not found with id=99"));

        mockMvc.perform(post("/api/admin/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"abonnementId": 99, "montant": 50.00,
                                 "methodePaiement": "CARTE_BANCAIRE",
                                 "statutPaiement": "COMPLETE"}
                                """))
                .andExpect(status().isNotFound());
    }
}
