package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.SubFamily;

import java.util.Optional;

public interface SubFamilyRepository extends JpaRepository<SubFamily, Long> {
    boolean existsByCode(String code);
    Optional<SubFamily> findByCode(String code);
}
