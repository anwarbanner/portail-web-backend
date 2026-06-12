package portail.web.backend.exemple.portail.web.backend.paiement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.common.entity.BaseTimestampEntity;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paiements")
@Getter
@Setter
@NoArgsConstructor
public class Paiement extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abonnement_id", nullable = false)
    private Abonnement abonnement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "methode_paiement", nullable = false, length = 50)
    private MethodePaiement methodePaiement;

    @Column(name = "reference_transaction", length = 200)
    private String referenceTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false, length = 30)
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;
}
