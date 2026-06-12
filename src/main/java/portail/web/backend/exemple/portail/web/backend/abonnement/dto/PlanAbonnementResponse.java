package portail.web.backend.exemple.portail.web.backend.abonnement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanAbonnementResponse(
        Long id,
        String nom,
        String description,
        BigDecimal prix,
        Integer dureeMois,
        Integer nombreConsultations,
        boolean illimite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
