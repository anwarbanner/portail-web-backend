package portail.web.backend.exemple.portail.web.backend.mockmvc_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.abonnement.controller.PlanAbonnementController;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.PlanAbonnementService;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests MockMvc du PlanAbonnementController — approche standalone (Spring Boot 4.x).
 * Vérifie les opérations CRUD sur les plans d'abonnement.
 */
@ExtendWith(MockitoExtension.class)
class PlanAbonnementControllerMvcTest {

    @Mock PlanAbonnementService planService;

    @InjectMocks PlanAbonnementController planController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(planController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PlanAbonnementResponse planResponse(Long id, String nom) {
        return new PlanAbonnementResponse(
                id, nom, "Description plan", new BigDecimal("99.00"),
                12, null, true, LocalDateTime.now(), LocalDateTime.now());
    }

    // ── GET /api/admin/plans ──────────────────────────────────────────────────

    @Test
    void listPlans_retourne200AvecListePlans() throws Exception {
        when(planService.findAll()).thenReturn(List.of(
                planResponse(1L, "Basique"),
                planResponse(2L, "Premium")));

        mockMvc.perform(get("/api/admin/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nom").value("Basique"))
                .andExpect(jsonPath("$[1].nom").value("Premium"));
    }

    @Test
    void listPlans_aucunPlan_retourne200ListeVide() throws Exception {
        when(planService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/admin/plans/{id} ─────────────────────────────────────────────

    @Test
    void getPlanById_planExistant_retourne200() throws Exception {
        when(planService.findById(1L)).thenReturn(planResponse(1L, "Premium"));

        mockMvc.perform(get("/api/admin/plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Premium"))
                .andExpect(jsonPath("$.illimite").value(true));
    }

    @Test
    void getPlanById_planInexistant_retourne404() throws Exception {
        when(planService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Plan not found with id=99"));

        mockMvc.perform(get("/api/admin/plans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Plan not found with id=99"));
    }

    // ── POST /api/admin/plans ─────────────────────────────────────────────────

    @Test
    void createPlan_donneesValides_retourne201() throws Exception {
        when(planService.creer(any())).thenReturn(planResponse(1L, "Enterprise"));

        mockMvc.perform(post("/api/admin/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Enterprise", "prix": 199.00, "dureeMois": 12, "illimite": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Enterprise"))
                .andExpect(jsonPath("$.illimite").value(true));
    }

    @Test
    void createPlan_nomVide_retourne400() throws Exception {
        mockMvc.perform(post("/api/admin/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "", "prix": 99.00, "dureeMois": 12, "illimite": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createPlan_illimiteAvecNombreConsultations_retourne400() throws Exception {
        when(planService.creer(any()))
                .thenThrow(new BadRequestException("Un plan illimité ne peut pas avoir un nombre de consultations"));

        mockMvc.perform(post("/api/admin/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Test", "prix": 99.00, "dureeMois": 12,
                                 "illimite": true, "nombreConsultations": 100}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/admin/plans/{id} ─────────────────────────────────────────────

    @Test
    void updatePlan_donneesValides_retourne200() throws Exception {
        when(planService.modifier(eq(1L), any())).thenReturn(planResponse(1L, "Premium Plus"));

        mockMvc.perform(put("/api/admin/plans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Premium Plus", "prix": 149.00, "dureeMois": 12, "illimite": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Premium Plus"));
    }

    @Test
    void updatePlan_planInexistant_retourne404() throws Exception {
        when(planService.modifier(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Plan not found with id=99"));

        mockMvc.perform(put("/api/admin/plans/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom": "Test", "prix": 99.00, "dureeMois": 12, "illimite": true}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/admin/plans/{id} ──────────────────────────────────────────

    @Test
    void deletePlan_planExistant_retourne204() throws Exception {
        doNothing().when(planService).supprimer(1L);

        mockMvc.perform(delete("/api/admin/plans/1"))
                .andExpect(status().isNoContent());

        verify(planService).supprimer(1L);
    }

    @Test
    void deletePlan_planInexistant_retourne404() throws Exception {
        doThrow(new ResourceNotFoundException("Plan not found with id=99"))
                .when(planService).supprimer(99L);

        mockMvc.perform(delete("/api/admin/plans/99"))
                .andExpect(status().isNotFound());
    }
}
