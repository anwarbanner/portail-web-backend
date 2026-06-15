package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.PlanAbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.PlanAbonnementService;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du PlanAbonnementService.
 * Vérifie la validation métier : cohérence illimité/consultations,
 * unicité du nom, CRUD complet.
 */
@ExtendWith(MockitoExtension.class)
class PlanAbonnementServiceTest {

    @Mock PlanAbonnementRepository planRepository;
    @Mock AbonnementMapper abonnementMapper;
    @InjectMocks PlanAbonnementService service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PlanAbonnementRequest request(boolean illimite, Integer nombreConsultations) {
        return new PlanAbonnementRequest(
                "Plan Test",
                "Description",
                new BigDecimal("99.99"),
                12,
                nombreConsultations,
                illimite
        );
    }

    private PlanAbonnement planEntity(Long id, String nom) {
        PlanAbonnement p = new PlanAbonnement();
        p.setId(id);
        p.setNom(nom);
        p.setPrix(new BigDecimal("99.99"));
        p.setDureeMois(12);
        p.setIllimite(false);
        p.setNombreConsultations(50);
        return p;
    }

    // ── creer() ──────────────────────────────────────────────────────────────

    @Test
    void creer_illimiteTrue_avecNombreConsultations_throwsBadRequest() {
        PlanAbonnementRequest req = request(true, 100); // incohérent
        assertThrows(BadRequestException.class, () -> service.creer(req));
        verifyNoInteractions(planRepository);
    }

    @Test
    void creer_illimiteFalse_sansNombreConsultations_throwsBadRequest() {
        PlanAbonnementRequest req = request(false, null); // incohérent
        assertThrows(BadRequestException.class, () -> service.creer(req));
        verifyNoInteractions(planRepository);
    }

    @Test
    void creer_nomDuplique_throwsBadRequest() {
        PlanAbonnementRequest req = request(false, 50);
        when(planRepository.existsByNom("Plan Test")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.creer(req));
        verify(planRepository, never()).save(any());
    }

    @Test
    void creer_valid_savesEtRetourne() {
        PlanAbonnementRequest req = request(false, 50);
        PlanAbonnement saved = planEntity(1L, "Plan Test");
        PlanAbonnementResponse expectedResponse = new PlanAbonnementResponse(
                1L, "Plan Test", "Description", new BigDecimal("99.99"), 12, 50, false, null, null);

        when(planRepository.existsByNom("Plan Test")).thenReturn(false);
        when(planRepository.save(any())).thenReturn(saved);
        when(abonnementMapper.toPlanResponse(saved)).thenReturn(expectedResponse);

        PlanAbonnementResponse result = service.creer(req);

        assertNotNull(result);
        assertEquals("Plan Test", result.nom());
        verify(planRepository).save(any(PlanAbonnement.class));
    }

    @Test
    void creer_illimiteTrue_sansNombreConsultations_savesAvecNombreConsultationsNull() {
        PlanAbonnementRequest req = new PlanAbonnementRequest(
                "Plan Illimite", null, new BigDecimal("199.99"), 12, null, true);
        PlanAbonnement saved = new PlanAbonnement();
        saved.setIllimite(true);

        when(planRepository.existsByNom("Plan Illimite")).thenReturn(false);
        when(planRepository.save(any())).thenReturn(saved);
        when(abonnementMapper.toPlanResponse(any())).thenReturn(
                new PlanAbonnementResponse(1L, "Plan Illimite", null, new BigDecimal("199.99"), 12, null, true, null, null));

        PlanAbonnementResponse result = service.creer(req);
        assertNotNull(result);
        assertTrue(result.illimite());
        assertNull(result.nombreConsultations());
    }

    // ── modifier() ───────────────────────────────────────────────────────────

    @Test
    void modifier_nomDupliqueSurAutreId_throwsBadRequest() {
        PlanAbonnementRequest req = request(false, 50);
        PlanAbonnement existing = planEntity(1L, "Autre Nom");

        when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(planRepository.existsByNomAndIdNot("Plan Test", 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.modifier(1L, req));
        verify(planRepository, never()).save(any());
    }

    @Test
    void modifier_valid_savesEtRetourne() {
        PlanAbonnementRequest req = request(false, 100);
        PlanAbonnement existing = planEntity(1L, "Plan Test");
        PlanAbonnementResponse expectedResponse = new PlanAbonnementResponse(
                1L, "Plan Test", "Description", new BigDecimal("99.99"), 12, 100, false, null, null);

        when(planRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(planRepository.existsByNomAndIdNot("Plan Test", 1L)).thenReturn(false);
        when(planRepository.save(existing)).thenReturn(existing);
        when(abonnementMapper.toPlanResponse(existing)).thenReturn(expectedResponse);

        PlanAbonnementResponse result = service.modifier(1L, req);

        assertNotNull(result);
        assertEquals(100, result.nombreConsultations());
        verify(planRepository).save(existing);
    }

    // ── supprimer() ──────────────────────────────────────────────────────────

    @Test
    void supprimer_notFound_throwsResourceNotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.supprimer(99L));
        verify(planRepository, never()).delete(any());
    }

    @Test
    void supprimer_valid_appelleDelete() {
        PlanAbonnement plan = planEntity(1L, "Plan Test");
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));

        assertDoesNotThrow(() -> service.supprimer(1L));
        verify(planRepository).delete(plan);
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
