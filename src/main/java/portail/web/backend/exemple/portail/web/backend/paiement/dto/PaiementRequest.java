package portail.web.backend.exemple.portail.web.backend.paiement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.MethodePaiement;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.StatutPaiement;

import java.math.BigDecimal;

public record PaiementRequest(
        @NotNull Long abonnementId,
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        @NotNull MethodePaiement methodePaiement,
        String referenceTransaction,
        @NotNull StatutPaiement statutPaiement
) {
}
