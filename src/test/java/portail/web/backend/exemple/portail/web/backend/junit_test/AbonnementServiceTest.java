package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.PlanAbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementService;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du AbonnementService.
 * Vérifie la création (statut PENDING, dateFin calculée, consultations),
 * l'annulation et la recherche par ID.
 */
@ExtendWith(MockitoExtension.class)
class AbonnementServiceTest {

    @Mock AbonnementRepository abonnementRepository;
    @Mock PlanAbonnementRepository planRepository;
    @Mock UserRepository userRepository;
    @Mock AbonnementMapper abonnementMapper;
    @InjectMocks AbonnementService service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("alice");
        return u;
    }

    private PlanAbonnement plan(Long id, boolean illimite, Integer nombreConsultations, int dureeMois) {
        PlanAbonnement p = new PlanAbonnement();
        p.setId(id);
        p.setNom("Plan Test");
        p.setPrix(new BigDecimal("99.99"));
        p.setDureeMois(dureeMois);
        p.setIllimite(illimite);
        p.setNombreConsultations(nombreConsultations);
        return p;
    }

    private AbonnementRequest request(Long userId, Long planId) {
        return new AbonnementRequest(userId, planId, LocalDate.of(2026, 1, 1));
    }

    // ── creer() ──────────────────────────────────────────────────────────────

    @Test
    void creer_userIntrouvable_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.creer(request(1L, 2L)));
    }

    @Test
    void creer_planIntrouvable_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(planRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.creer(request(1L, 2L)));
    }

    @Test
    void creer_statutEstPending() {
        User u = user(1L);
        PlanAbonnement p = plan(2L, false, 50, 12);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findById(2L)).thenReturn(Optional.of(p));
        when(abonnementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.creer(request(1L, 2L));

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(abonnementRepository).save(captor.capture());
        assertEquals(StatutAbonnement.PENDING, captor.getValue().getStatut());
    }

    @Test
    void creer_dateFinCalculeeSelonDureeMois() {
        User u = user(1L);
        PlanAbonnement p = plan(2L, false, 50, 6); // 6 mois
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findById(2L)).thenReturn(Optional.of(p));
        when(abonnementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.creer(request(1L, 2L));

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(abonnementRepository).save(captor.capture());

        LocalDate expectedDateFin = LocalDate.of(2026, 1, 1).plusMonths(6);
        assertEquals(expectedDateFin, captor.getValue().getDateFin());
    }

    @Test
    void creer_planNonIllimite_consultationsRestantesInitialisees() {
        User u = user(1L);
        PlanAbonnement p = plan(2L, false, 50, 12);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findById(2L)).thenReturn(Optional.of(p));
        when(abonnementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.creer(request(1L, 2L));

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(abonnementRepository).save(captor.capture());
        assertEquals(50, captor.getValue().getConsultationsRestantes());
    }

    @Test
    void creer_planIllimite_consultationsRestantesNull() {
        User u = user(1L);
        PlanAbonnement p = plan(2L, true, null, 12);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(planRepository.findById(2L)).thenReturn(Optional.of(p));
        when(abonnementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.creer(request(1L, 2L));

        ArgumentCaptor<Abonnement> captor = ArgumentCaptor.forClass(Abonnement.class);
        verify(abonnementRepository).save(captor.capture());
        assertNull(captor.getValue().getConsultationsRestantes());
    }

    // ── annuler() ────────────────────────────────────────────────────────────

    @Test
    void annuler_setStatutCancelled() {
        Abonnement abo = new Abonnement();
        abo.setStatut(StatutAbonnement.ACTIVE);
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abo));

        service.annuler(1L);

        assertEquals(StatutAbonnement.CANCELLED, abo.getStatut());
        verify(abonnementRepository).save(abo);
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(abonnementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
