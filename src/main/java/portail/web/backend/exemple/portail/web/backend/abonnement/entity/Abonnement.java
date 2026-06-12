package portail.web.backend.exemple.portail.web.backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portail.web.backend.exemple.portail.web.backend.common.entity.BaseTimestampEntity;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.time.LocalDate;

@Entity
@Table(name = "abonnements")
@Getter
@Setter
@NoArgsConstructor
public class Abonnement extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanAbonnement plan;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAbonnement statut = StatutAbonnement.PENDING;

    @Column(name = "consultations_restantes")
    private Integer consultationsRestantes; // null si plan illimite

    public boolean isActif() {
        return statut == StatutAbonnement.ACTIVE
                && !dateFin.isBefore(LocalDate.now());
    }
}
