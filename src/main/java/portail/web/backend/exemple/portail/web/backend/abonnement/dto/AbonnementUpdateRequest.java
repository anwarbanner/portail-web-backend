package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import jakarta.validation.constraints.NotNull;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;

import java.time.LocalDate;

public record AbonnementUpdateRequest(
        @NotNull LocalDate dateFin,
        Integer consultationsRestantes, // null = ne pas modifier
        @NotNull StatutAbonnement statut
) {
}
