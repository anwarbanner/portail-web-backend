package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonAbonnementResponse(
        Long id,
        String planNom,
        String planDescription,
        BigDecimal prix,
        Integer dureeMois,
        Integer nombreConsultations,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        Integer consultationsRestantes,
        boolean illimite,
        boolean actif
) {
}
