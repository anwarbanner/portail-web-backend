package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementRequest;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.MethodePaiement;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.Paiement;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.StatutPaiement;
import portail.web.backend.exemple.portail.web.backend.paiement.mapper.PaiementMapper;
import portail.web.backend.exemple.portail.web.backend.paiement.repository.PaiementRepository;
import portail.web.backend.exemple.portail.web.backend.paiement.service.PaiementService;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du PaiementService.
 * Vérifie la règle clé : paiement COMPLETE + abonnement PENDING → abonnement ACTIVE.
 */
@ExtendWith(MockitoExtension.class)
class PaiementServiceTest {

    @Mock PaiementRepository paiementRepository;
    @Mock AbonnementRepository abonnementRepository;
    @Mock PaiementMapper paiementMapper;
    @InjectMocks PaiementService service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Abonnement abonnement(Long id, StatutAbonnement statut) {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Abonnement a = new Abonnement();
        a.setId(id);
        a.setUser(user);
        a.setStatut(statut);
        a.setDateFin(LocalDate.now().plusDays(30));
        return a;
    }

    private PaiementRequest request(Long abonnementId, StatutPaiement statut) {
        return new PaiementRequest(
                abonnementId,
                new BigDecimal("99.99"),
                MethodePaiement.CARTE_BANCAIRE,
                "REF-001",
                statut
        );
    }

    // ── enregistrer() ────────────────────────────────────────────────────────

    @Test
    void enregistrer_abonnementIntrouvable_throwsResourceNotFound() {
        when(abonnementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.enregistrer(request(99L, StatutPaiement.COMPLETE)));
    }

    @Test
    void enregistrer_abonnementAnnule_throwsBadRequest() {
        when(abonnementRepository.findById(1L))
                .thenReturn(Optional.of(abonnement(1L, StatutAbonnement.CANCELLED)));

        assertThrows(BadRequestException.class,
                () -> service.enregistrer(request(1L, StatutPaiement.COMPLETE)));

        verify(paiementRepository, never()).save(any());
    }

    @Test
    void enregistrer_paiementComplete_abonnementPending_activeLAbonnement() {
        Abonnement abo = abonnement(1L, StatutAbonnement.PENDING);
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abo));
        when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enregistrer(request(1L, StatutPaiement.COMPLETE));

        assertEquals(StatutAbonnement.ACTIVE, abo.getStatut());
        verify(abonnementRepository).save(abo);
    }

    @Test
    void enregistrer_paiementEchoue_abonnementResteInchange() {
        Abonnement abo = abonnement(1L, StatutAbonnement.PENDING);
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abo));
        when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enregistrer(request(1L, StatutPaiement.ECHOUE));

        assertEquals(StatutAbonnement.PENDING, abo.getStatut());
        verify(abonnementRepository, never()).save(abo);
    }

    @Test
    void enregistrer_paiementComplete_abonnementDejaActive_resteActive() {
        Abonnement abo = abonnement(1L, StatutAbonnement.ACTIVE);
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abo));
        when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enregistrer(request(1L, StatutPaiement.COMPLETE));

        // Abonnement déjà ACTIVE, le guard ne le repasse pas
        assertEquals(StatutAbonnement.ACTIVE, abo.getStatut());
        verify(abonnementRepository, never()).save(abo);
    }

    @Test
    void enregistrer_paiementSauvegarde() {
        Abonnement abo = abonnement(1L, StatutAbonnement.ACTIVE);
        when(abonnementRepository.findById(1L)).thenReturn(Optional.of(abo));
        when(paiementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.enregistrer(request(1L, StatutPaiement.COMPLETE));

        verify(paiementRepository).save(any(Paiement.class));
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(paiementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
