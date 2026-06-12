package portail.web.backend.exemple.portail.web.backend.abonnement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.AbonnementUpdateRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.MonAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementService;
import portail.web.backend.exemple.portail.web.backend.consultation.dto.ConsultationResponse;
import portail.web.backend.exemple.portail.web.backend.consultation.service.ConsultationService;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

@Tag(name = "Abonnements")
@RestController
@RequiredArgsConstructor
public class AbonnementController {

    private final AbonnementService abonnementService;
    private final ConsultationService consultationService;
    private final UserRepository userRepository;

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Liste tous les abonnements")
    @GetMapping("/api/admin/abonnements")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AbonnementResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return abonnementService.findAll(pageable);
    }

    @Operation(summary = "Détail d'un abonnement")
    @GetMapping("/api/admin/abonnements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AbonnementResponse findById(@PathVariable Long id) {
        return abonnementService.findById(id);
    }

    @Operation(summary = "Créer un abonnement pour un utilisateur (statut PENDING)")
    @PostMapping("/api/admin/abonnements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AbonnementResponse> creer(@Valid @RequestBody AbonnementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonnementService.creer(request));
    }

    @Operation(summary = "Modifier un abonnement (prolonger, ajuster consultations, changer statut)")
    @PutMapping("/api/admin/abonnements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AbonnementResponse modifier(@PathVariable Long id,
                                       @Valid @RequestBody AbonnementUpdateRequest request) {
        return abonnementService.modifier(id, request);
    }

    @Operation(summary = "Annuler un abonnement")
    @DeleteMapping("/api/admin/abonnements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> annuler(@PathVariable Long id) {
        abonnementService.annuler(id);
        return ResponseEntity.noContent().build();
    }

    // ── Client ────────────────────────────────────────────────────────────────

    @Operation(summary = "Mon abonnement actif (204 si aucun)")
    @GetMapping("/api/abonnements/me")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<MonAbonnementResponse> monAbonnement(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return abonnementService.findMonActif(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Mon historique de consultations")
    @GetMapping("/api/abonnements/me/consultations")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<ConsultationResponse> mesConsultations(Authentication authentication,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        Long userId = resolveUserId(authentication);
        return consultationService.findByUser(userId, pageable);
    }

    private Long resolveUserId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"))
                .getId();
    }
}
