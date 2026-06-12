package portail.web.backend.exemple.portail.web.backend.paiement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaiementResponse(
        Long id,
        Long abonnementId,
        Long userId,
        String username,
        BigDecimal montant,
        LocalDateTime datePaiement,
        String methodePaiement,
        String referenceTransaction,
        String statutPaiement,
        LocalDateTime createdAt
) {
}
