package portail.web.backend.exemple.portail.web.backend.abonnement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementUpdateRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.MonAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.PlanAbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AbonnementService {

    private final AbonnementRepository abonnementRepository;
    private final PlanAbonnementRepository planRepository;
    private final UserRepository userRepository;
    private final AbonnementMapper abonnementMapper;

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AbonnementResponse> findAll(Pageable pageable) {
        return abonnementRepository.findAll(pageable).map(abonnementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AbonnementResponse findById(Long id) {
        return abonnementMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public AbonnementResponse creer(AbonnementRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec id=" + request.userId()));

        PlanAbonnement plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan non trouvé avec id=" + request.planId()));

        Abonnement abonnement = new Abonnement();
        abonnement.setUser(user);
        abonnement.setPlan(plan);
        abonnement.setDateDebut(request.dateDebut());
        abonnement.setDateFin(request.dateDebut().plusMonths(plan.getDureeMois()));
        abonnement.setStatut(StatutAbonnement.PENDING);
        abonnement.setConsultationsRestantes(plan.isIllimite() ? null : plan.getNombreConsultations());

        return abonnementMapper.toResponse(abonnementRepository.save(abonnement));
    }

    @Transactional
    public AbonnementResponse modifier(Long id, AbonnementUpdateRequest request) {
        Abonnement abonnement = getOrThrow(id);
        abonnement.setDateFin(request.dateFin());
        abonnement.setStatut(request.statut());
        if (request.consultationsRestantes() != null) {
            abonnement.setConsultationsRestantes(request.consultationsRestantes());
        }
        return abonnementMapper.toResponse(abonnementRepository.save(abonnement));
    }

    @Transactional
    public void annuler(Long id) {
        Abonnement abonnement = getOrThrow(id);
        abonnement.setStatut(StatutAbonnement.CANCELLED);
        abonnementRepository.save(abonnement);
    }

    // ── Client ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<MonAbonnementResponse> findMonActif(Long userId) {
        return abonnementRepository
                .findActifByUserId(userId, LocalDate.now())
                .map(abonnementMapper::toMonResponse);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    Abonnement getOrThrow(Long id) {
        return abonnementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement non trouvé avec id=" + id));
    }
}
