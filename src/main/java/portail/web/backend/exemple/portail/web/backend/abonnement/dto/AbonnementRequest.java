package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AbonnementRequest(
        @NotNull Long userId,
        @NotNull Long planId,
        @NotNull LocalDate dateDebut
) {
}
