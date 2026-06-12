package portail.web.backend.exemple.portail.web.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.dto.LookupRequest;
import portail.web.backend.exemple.portail.web.backend.dto.LookupResponse;
import portail.web.backend.exemple.portail.web.backend.service.LookupService;
import portail.web.backend.exemple.portail.web.backend.service.LookupType;

@RestController
@RequestMapping("/api/lookups/{type}")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @Operation(summary = "List lookup entries by type")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<LookupResponse> findAll(@PathVariable LookupType type, @PageableDefault(size = 20) Pageable pageable) {
        return lookupService.findAll(type, pageable);
    }

    @Operation(summary = "Get lookup entry by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public LookupResponse findById(@PathVariable LookupType type, @PathVariable Long id) {
        return lookupService.findById(type, id);
    }

    @Operation(summary = "Create lookup entry")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<LookupResponse> create(@PathVariable LookupType type, @Valid @RequestBody LookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lookupService.create(type, request));
    }

    @Operation(summary = "Update lookup entry")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LookupResponse update(@PathVariable LookupType type, @PathVariable Long id, @Valid @RequestBody LookupRequest request) {
        return lookupService.update(type, id, request);
    }

    @Operation(summary = "Delete lookup entry")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable LookupType type, @PathVariable Long id) {
        lookupService.delete(type, id);
        return ResponseEntity.noContent().build();
    }
}

