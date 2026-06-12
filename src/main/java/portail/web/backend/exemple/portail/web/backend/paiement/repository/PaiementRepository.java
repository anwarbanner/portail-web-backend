package portail.web.backend.exemple.portail.web.backend.paiement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.Paiement;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Page<Paiement> findByAbonnementId(Long abonnementId, Pageable pageable);
    Page<Paiement> findByUserId(Long userId, Pageable pageable);
}
