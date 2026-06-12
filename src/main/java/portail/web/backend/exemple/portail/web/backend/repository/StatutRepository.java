package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.Statut;

import java.util.Optional;

public interface StatutRepository extends JpaRepository<Statut, Long> {
    boolean existsByCode(String code);
    Optional<Statut> findByCode(String code);
}

