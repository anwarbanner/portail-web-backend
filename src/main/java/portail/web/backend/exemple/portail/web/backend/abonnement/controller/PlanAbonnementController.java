package portail.web.backend.exemple.portail.web.backend.abonnement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementRequest;
import portail.web.backend.exemple.portail.web.backend.abonnement.dto.PlanAbonnementResponse;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.PlanAbonnementService;

import java.util.List;

@Tag(name = "Plans d'abonnement")
@RestController
@RequestMapping("/api/admin/plans")
@RequiredArgsConstructor
public class PlanAbonnementController {

    private final PlanAbonnementService planService;

    @Operation(summary = "Liste tous les plans (public)")
    @GetMapping
    public List<PlanAbonnementResponse> findAll() {
        return planService.findAll();
    }

    @Operation(summary = "Détail d'un plan")
    @GetMapping("/{id}")
    public PlanAbonnementResponse findById(@PathVariable Long id) {
        return planService.findById(id);
    }

    @Operation(summary = "Créer un plan")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanAbonnementResponse> creer(@Valid @RequestBody PlanAbonnementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.creer(request));
    }

    @Operation(summary = "Modifier un plan")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlanAbonnementResponse modifier(@PathVariable Long id,
                                           @Valid @RequestBody PlanAbonnementRequest request) {
        return planService.modifier(id, request);
    }

    @Operation(summary = "Supprimer un plan")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        planService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
