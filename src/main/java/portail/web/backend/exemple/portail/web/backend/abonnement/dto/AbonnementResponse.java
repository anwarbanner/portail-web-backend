package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AbonnementResponse(
        Long id,
        Long userId,
        String username,
        Long planId,
        String planNom,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        Integer consultationsRestantes,
        boolean illimite,
        boolean actif,
        LocalDateTime createdAt
) {
}
