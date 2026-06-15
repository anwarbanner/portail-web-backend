package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementGuard;
import portail.web.backend.exemple.portail.web.backend.consultation.service.ConsultationService;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.exception.ConsultationLimitExceededException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.exception.SubscriptionRequiredException;
import portail.web.backend.exemple.portail.web.backend.repository.NormeRepository;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du AbonnementGuard.
 * Vérifie la logique de contrôle d'accès aux PDF : admin bypass,
 * abonnement actif, limite de consultations, décrément du compteur.
 */
@ExtendWith(MockitoExtension.class)
class AbonnementGuardTest {

    @Mock AbonnementRepository abonnementRepository;
    @Mock NormeRepository normeRepository;
    @Mock UserRepository userRepository;
    @Mock ConsultationService consultationService;
    @InjectMocks AbonnementGuard guard;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Authentication adminAuth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities())
                .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return auth;
    }

    private Authentication userAuth(String username) {
        Authentication auth = mock(Authentication.class);
        // getAuthorities() est toujours appelé (isAdmin check)
        // getName() n'est pas appelé dans tous les tests → lenient pour éviter UnnecessaryStubbing
        lenient().when(auth.getAuthorities()).thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_USER")));
        lenient().when(auth.getName()).thenReturn(username);
        return auth;
    }

    private Norme normeIncluded() {
        Norme n = new Norme();
        n.setIncludedInSubscription(true);
        return n;
    }

    private Norme normeExcluded() {
        Norme n = new Norme();
        n.setIncludedInSubscription(false);
        return n;
    }

    private User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Abonnement abonnement(boolean illimite, Integer consultationsRestantes) {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setIllimite(illimite);
        plan.setNombreConsultations(illimite ? null : 10);

        Abonnement a = new Abonnement();
        a.setPlan(plan);
        a.setStatut(StatutAbonnement.ACTIVE);
        a.setDateFin(LocalDate.now().plusDays(30));
        a.setConsultationsRestantes(consultationsRestantes);
        return a;
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    void admin_bypass_retourneSansVerification() {
        // Un admin peut télécharger sans abonnement
        assertDoesNotThrow(() -> guard.verifierEtEnregistrer(adminAuth(), 1L));
        verifyNoInteractions(normeRepository, userRepository, abonnementRepository, consultationService);
    }

    @Test
    void normeIntrouvable_throwsResourceNotFoundException() {
        when(normeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> guard.verifierEtEnregistrer(userAuth("alice"), 99L));
    }

    @Test
    void normeNonIncluse_retourneSansVerifierAbonnement() {
        when(normeRepository.findById(1L)).thenReturn(Optional.of(normeExcluded()));

        assertDoesNotThrow(() -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));
        verifyNoInteractions(userRepository, abonnementRepository, consultationService);
    }

    @Test
    void userIntrouvable_throwsResourceNotFoundException() {
        when(normeRepository.findById(1L)).thenReturn(Optional.of(normeIncluded()));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));
    }

    @Test
    void sansAbonnementActif_throwsSubscriptionRequiredException() {
        User alice = user(10L, "alice");
        when(normeRepository.findById(1L)).thenReturn(Optional.of(normeIncluded()));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(abonnementRepository.findActifByUserId(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThrows(SubscriptionRequiredException.class,
                () -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));
    }

    @Test
    void planIllimite_sansDecrementEtConsultationEnregistree() {
        Norme norme = normeIncluded();
        User alice = user(10L, "alice");
        Abonnement abo = abonnement(true, null); // illimite

        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(abonnementRepository.findActifByUserId(eq(10L), any())).thenReturn(Optional.of(abo));

        assertDoesNotThrow(() -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));

        verify(abonnementRepository, never()).save(any());
        verify(consultationService).enregistrer(alice, norme);
    }

    @Test
    void planLimite_consultationsRestantes1_decrementEtSave() {
        Norme norme = normeIncluded();
        User alice = user(10L, "alice");
        Abonnement abo = abonnement(false, 1); // 1 consultation restante

        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(abonnementRepository.findActifByUserId(eq(10L), any())).thenReturn(Optional.of(abo));

        assertDoesNotThrow(() -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));

        // consultationsRestantes doit être decrementé à 0
        verify(abonnementRepository).save(abo);
        assert abo.getConsultationsRestantes() == 0;
        verify(consultationService).enregistrer(alice, norme);
    }

    @Test
    void planLimite_consultationsRestantes0_throwsConsultationLimitExceededException() {
        Norme norme = normeIncluded();
        User alice = user(10L, "alice");
        Abonnement abo = abonnement(false, 0); // épuisé

        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(abonnementRepository.findActifByUserId(eq(10L), any())).thenReturn(Optional.of(abo));

        assertThrows(ConsultationLimitExceededException.class,
                () -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));

        verify(abonnementRepository, never()).save(any());
        verifyNoInteractions(consultationService);
    }

    @Test
    void planLimite_consultationsRestantesNull_throwsConsultationLimitExceededException() {
        Norme norme = normeIncluded();
        User alice = user(10L, "alice");
        Abonnement abo = abonnement(false, null); // null = traité comme 0

        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(abonnementRepository.findActifByUserId(eq(10L), any())).thenReturn(Optional.of(abo));

        assertThrows(ConsultationLimitExceededException.class,
                () -> guard.verifierEtEnregistrer(userAuth("alice"), 1L));
    }
}
