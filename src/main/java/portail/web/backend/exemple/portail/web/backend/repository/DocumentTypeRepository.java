package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.DocumentType;

import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    boolean existsByCode(String code);
    Optional<DocumentType> findByCode(String code);
}
