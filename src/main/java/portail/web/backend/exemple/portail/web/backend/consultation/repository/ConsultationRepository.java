package portail.web.backend.exemple.portail.web.backend.consultation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.consultation.entity.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Page<Consultation> findByUserIdOrderByDateConsultationDesc(Long userId, Pageable pageable);
}
