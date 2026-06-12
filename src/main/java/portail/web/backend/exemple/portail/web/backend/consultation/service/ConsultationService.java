package portail.web.backend.exemple.portail.web.backend.consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.consultation.dto.ConsultationResponse;
import portail.web.backend.exemple.portail.web.backend.consultation.entity.Consultation;
import portail.web.backend.exemple.portail.web.backend.consultation.repository.ConsultationRepository;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.user.User;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AbonnementMapper abonnementMapper;

    @Transactional
    public void enregistrer(User user, Norme norme) {
        Consultation consultation = new Consultation();
        consultation.setUser(user);
        consultation.setNorme(norme);
        consultation.setDateConsultation(LocalDateTime.now());
        consultationRepository.save(consultation);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> findByUser(Long userId, Pageable pageable) {
        return consultationRepository
                .findByUserIdOrderByDateConsultationDesc(userId, pageable)
                .map(abonnementMapper::toConsultationResponse);
    }
}
