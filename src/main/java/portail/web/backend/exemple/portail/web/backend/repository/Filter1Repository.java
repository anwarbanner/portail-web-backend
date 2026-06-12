package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.Filter1;

import java.util.Optional;

public interface Filter1Repository extends JpaRepository<Filter1, Long> {
    boolean existsByCode(String code);
    Optional<Filter1> findByCode(String code);
}
