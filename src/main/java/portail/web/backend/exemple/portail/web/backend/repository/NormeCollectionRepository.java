package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.NormeCollection;

import java.util.Optional;

public interface NormeCollectionRepository extends JpaRepository<NormeCollection, Long> {
    boolean existsByCode(String code);
    Optional<NormeCollection> findByCode(String code);
}
