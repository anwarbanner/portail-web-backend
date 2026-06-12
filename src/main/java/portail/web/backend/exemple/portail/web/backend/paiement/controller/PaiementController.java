package portail.web.backend.exemple.portail.web.backend.paiement.controller;

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
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementRequest;
import portail.web.backend.exemple.portail.web.backend.paiement.dto.PaiementResponse;
import portail.web.backend.exemple.portail.web.backend.paiement.service.PaiementService;

@Tag(name = "Admin — Paiements")
@RestController
@RequestMapping("/api/admin/paiements")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PaiementController {

    private final PaiementService paiementService;

    @Operation(summary = "Liste tous les paiements")
    @GetMapping
    public Page<PaiementResponse> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return paiementService.findAll(pageable);
    }

    @Operation(summary = "Paiements d'un abonnement")
    @GetMapping("/abonnement/{abonnementId}")
    public Page<PaiementResponse> findByAbonnement(@PathVariable Long abonnementId,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return paiementService.findByAbonnement(abonnementId, pageable);
    }

    @Operation(summary = "Détail d'un paiement")
    @GetMapping("/{id}")
    public PaiementResponse findById(@PathVariable Long id) {
        return paiementService.findById(id);
    }

    @Operation(summary = "Enregistrer un paiement (COMPLETE active l'abonnement automatiquement)")
    @PostMapping
    public ResponseEntity<PaiementResponse> enregistrer(@Valid @RequestBody PaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementService.enregistrer(request));
    }
}
