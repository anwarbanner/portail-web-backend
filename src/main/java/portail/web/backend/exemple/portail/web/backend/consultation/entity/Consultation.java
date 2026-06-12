package portail.web.backend.exemple.portail.web.backend.consultation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "norme_id", nullable = false)
    private Norme norme;

    @Column(name = "date_consultation", nullable = false)
    private LocalDateTime dateConsultation;
}
