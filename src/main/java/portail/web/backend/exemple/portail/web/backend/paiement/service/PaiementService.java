package portail.web.backend.exemple.portail.web.backend.paiement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.StatutAbonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementRequest;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementResponse;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.Paiement;
import portail.web.backend.exemple.portail.web.backend.paiement.entity.StatutPaiement;
import portail.web.backend.exemple.portail.web.backend.paiement.mapper.PaiementMapper;
import portail.web.backend.exemple.portail.web.backend.paiement.repository.PaiementRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final AbonnementRepository abonnementRepository;
    private final PaiementMapper paiementMapper;

    @Transactional(readOnly = true)
    public Page<PaiementResponse> findAll(Pageable pageable) {
        return paiementRepository.findAll(pageable).map(paiementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PaiementResponse> findByAbonnement(Long abonnementId, Pageable pageable) {
        return paiementRepository.findByAbonnementId(abonnementId, pageable)
                .map(paiementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaiementResponse findById(Long id) {
        return paiementMapper.toResponse(getOrThrow(id));
    }

    /**
     * Enregistre un paiement.
     * Si le statut est COMPLETE, l'abonnement associé passe automatiquement à ACTIVE.
     */
    @Transactional
    public PaiementResponse enregistrer(PaiementRequest request) {
        Abonnement abonnement = abonnementRepository.findById(request.abonnementId())
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement non trouvé avec id=" + request.abonnementId()));

        if (abonnement.getStatut() == StatutAbonnement.CANCELLED) {
            throw new BadRequestException("Impossible d'enregistrer un paiement pour un abonnement annulé");
        }

        Paiement paiement = new Paiement();
        paiement.setAbonnement(abonnement);
        paiement.setUser(abonnement.getUser());
        paiement.setMontant(request.montant());
        paiement.setDatePaiement(LocalDateTime.now());
        paiement.setMethodePaiement(request.methodePaiement());
        paiement.setReferenceTransaction(request.referenceTransaction());
        paiement.setStatutPaiement(request.statutPaiement());

        if (request.statutPaiement() == StatutPaiement.COMPLETE
                && abonnement.getStatut() == StatutAbonnement.PENDING) {
            abonnement.setStatut(StatutAbonnement.ACTIVE);
            abonnementRepository.save(abonnement);
        }

        return paiementMapper.toResponse(paiementRepository.save(paiement));
    }

    private Paiement getOrThrow(Long id) {
        return paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement non trouvé avec id=" + id));
    }
}
