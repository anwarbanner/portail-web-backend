package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.IcsLevel1;

import java.util.Optional;

public interface IcsLevel1Repository extends JpaRepository<IcsLevel1, Long> {
    boolean existsByCode(String code);
    Optional<IcsLevel1> findByCode(String code);
}
