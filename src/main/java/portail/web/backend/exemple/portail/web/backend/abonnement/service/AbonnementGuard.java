package portail.web.backend.exemple.portail.web.backend.abonnement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.abonnement.entity.Abonnement;
import portail.web.backend.exemple.portail.web.backend.abonnement.repository.AbonnementRepository;
import portail.web.backend.exemple.portail.web.backend.consultation.service.ConsultationService;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.exception.ConsultationLimitExceededException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.exception.SubscriptionRequiredException;
import portail.web.backend.exemple.portail.web.backend.repository.NormeRepository;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

import java.time.LocalDate;

/**
 * Contrôle d'accès aux PDF : vérifie l'abonnement actif, les consultations restantes,
 * décrémente le compteur et enregistre la consultation.
 * Les admins passent sans vérification.
 */
@Component
@RequiredArgsConstructor
public class AbonnementGuard {

    private final AbonnementRepository abonnementRepository;
    private final NormeRepository normeRepository;
    private final UserRepository userRepository;
    private final ConsultationService consultationService;

    @Transactional
    public void verifierEtEnregistrer(Authentication authentication, Long normeId) {
        if (isAdmin(authentication)) {
            return;
        }

        Norme norme = normeRepository.findById(normeId)
                .orElseThrow(() -> new ResourceNotFoundException("Norme non trouvée avec id=" + normeId));

        if (!norme.isIncludedInSubscription()) {
            return;
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Abonnement abonnement = abonnementRepository
                .findActifByUserId(user.getId(), LocalDate.now())
                .orElseThrow(() -> new SubscriptionRequiredException(
                        "Un abonnement actif est requis pour télécharger ce PDF"));

        if (!abonnement.getPlan().isIllimite()) {
            if (abonnement.getConsultationsRestantes() == null
                    || abonnement.getConsultationsRestantes() <= 0) {
                throw new ConsultationLimitExceededException(
                        "Vous avez atteint la limite de consultations de votre plan");
            }
            abonnement.setConsultationsRestantes(abonnement.getConsultationsRestantes() - 1);
            abonnementRepository.save(abonnement);
        }

        consultationService.enregistrer(user, norme);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
