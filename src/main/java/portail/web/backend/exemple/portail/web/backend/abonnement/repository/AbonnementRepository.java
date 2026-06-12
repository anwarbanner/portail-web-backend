package portail.web.backend.exemple.portail.web.backend.abonnement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    @Query("""
            SELECT a FROM Abonnement a
            WHERE a.user.id = :userId
              AND a.statut = 'ACTIVE'
              AND a.dateFin >= :today
            ORDER BY a.dateFin DESC
            """)
    Optional<Abonnement> findActifByUserId(@Param("userId") Long userId,
                                           @Param("today") LocalDate today);

    Page<Abonnement> findByUserId(Long userId, Pageable pageable);

    Page<Abonnement> findByStatut(StatutAbonnement statut, Pageable pageable);

    List<Abonnement> findByUserId(Long userId);
}
