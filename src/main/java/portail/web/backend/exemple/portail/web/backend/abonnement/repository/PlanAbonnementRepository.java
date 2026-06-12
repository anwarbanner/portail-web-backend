package portail.web.backend.exemple.portail.web.backend.abonnement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;

public interface PlanAbonnementRepository extends JpaRepository<PlanAbonnement, Long> {
    boolean existsByNom(String nom);
    boolean existsByNomAndIdNot(String nom, Long id);
}
