package portail.web.backend.exemple.portail.web.backend.abonnement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portail.web.backend.exemple.portail.web.backend.common.entity.BaseTimestampEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "plans_abonnement")
@Getter
@Setter
@NoArgsConstructor
public class PlanAbonnement extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal prix;

    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois;

    @Column(name = "nombre_consultations")
    private Integer nombreConsultations; // null si illimite

    @Column(nullable = false)
    private boolean illimite = false;
}
