package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests la méthode isActif() de l'entité Abonnement.
 * Vérifie toutes les combinaisons statut × dateFin.
 */
class AbonnementIsActifTest {

    private Abonnement abonnement(StatutAbonnement statut, LocalDate dateFin) {
        Abonnement a = new Abonnement();
        a.setStatut(statut);
        a.setDateFin(dateFin);
        return a;
    }

    @Test
    void active_dateFinFuture_isTrue() {
        assertTrue(abonnement(StatutAbonnement.ACTIVE, LocalDate.now().plusDays(1)).isActif());
    }

    @Test
    void active_dateFinAujourdhui_isTrue() {
        // dateFin = today => !today.isBefore(today) => true
        assertTrue(abonnement(StatutAbonnement.ACTIVE, LocalDate.now()).isActif());
    }

    @Test
    void active_dateFinPasse_isFalse() {
        assertFalse(abonnement(StatutAbonnement.ACTIVE, LocalDate.now().minusDays(1)).isActif());
    }

    @Test
    void pending_dateFinFuture_isFalse() {
        assertFalse(abonnement(StatutAbonnement.PENDING, LocalDate.now().plusDays(1)).isActif());
    }

    @Test
    void expired_dateFinFuture_isFalse() {
        assertFalse(abonnement(StatutAbonnement.EXPIRED, LocalDate.now().plusDays(1)).isActif());
    }

    @Test
    void cancelled_dateFinFuture_isFalse() {
        assertFalse(abonnement(StatutAbonnement.CANCELLED, LocalDate.now().plusDays(1)).isActif());
    }
}
