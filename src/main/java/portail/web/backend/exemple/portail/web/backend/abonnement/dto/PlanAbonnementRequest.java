package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PlanAbonnementRequest(
        @NotBlank @Size(max = 100) String nom,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal prix,
        @NotNull @Min(1) Integer dureeMois,
        @Min(1) Integer nombreConsultations, // null si illimite
        @NotNull Boolean illimite
) {
}
