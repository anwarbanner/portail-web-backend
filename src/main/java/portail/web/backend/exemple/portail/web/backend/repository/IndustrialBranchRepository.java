package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.IndustrialBranch;

import java.util.Optional;

public interface IndustrialBranchRepository extends JpaRepository<IndustrialBranch, Long> {
    boolean existsByCode(String code);
    Optional<IndustrialBranch> findByCode(String code);
}
