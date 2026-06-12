package portail.web.backend.exemple.portail.web.backend.abonnement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.PlanAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.mapper.AbonnementMapper;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.PlanAbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanAbonnementService {

    private final PlanAbonnementRepository planRepository;
    private final AbonnementMapper abonnementMapper;

    @Transactional(readOnly = true)
    public List<PlanAbonnementResponse> findAll() {
        return planRepository.findAll().stream()
                .map(abonnementMapper::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanAbonnementResponse findById(Long id) {
        return abonnementMapper.toPlanResponse(getOrThrow(id));
    }

    @Transactional
    public PlanAbonnementResponse creer(PlanAbonnementRequest request) {
        validerIllimite(request);
        if (planRepository.existsByNom(request.nom())) {
            throw new BadRequestException("Un plan avec le nom '" + request.nom() + "' existe déjà");
        }
        PlanAbonnement plan = new PlanAbonnement();
        appliquerRequest(plan, request);
        return abonnementMapper.toPlanResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanAbonnementResponse modifier(Long id, PlanAbonnementRequest request) {
        validerIllimite(request);
        PlanAbonnement plan = getOrThrow(id);
        if (planRepository.existsByNomAndIdNot(request.nom(), id)) {
            throw new BadRequestException("Un plan avec le nom '" + request.nom() + "' existe déjà");
        }
        appliquerRequest(plan, request);
        return abonnementMapper.toPlanResponse(planRepository.save(plan));
    }

    @Transactional
    public void supprimer(Long id) {
        planRepository.delete(getOrThrow(id));
    }

    private void validerIllimite(PlanAbonnementRequest request) {
        if (Boolean.TRUE.equals(request.illimite()) && request.nombreConsultations() != null) {
            throw new BadRequestException("Un plan illimité ne peut pas avoir un nombre de consultations");
        }
        if (Boolean.FALSE.equals(request.illimite()) && request.nombreConsultations() == null) {
            throw new BadRequestException("Un plan non illimité doit avoir un nombre de consultations");
        }
    }

    private void appliquerRequest(PlanAbonnement plan, PlanAbonnementRequest request) {
        plan.setNom(request.nom());
        plan.setDescription(request.description());
        plan.setPrix(request.prix());
        plan.setDureeMois(request.dureeMois());
        plan.setIllimite(Boolean.TRUE.equals(request.illimite()));
        plan.setNombreConsultations(plan.isIllimite() ? null : request.nombreConsultations());
    }

    private PlanAbonnement getOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan non trouvé avec id=" + id));
    }
}
