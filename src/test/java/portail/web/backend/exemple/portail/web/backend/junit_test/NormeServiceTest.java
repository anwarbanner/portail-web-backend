package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portail.web.backend.exemple.portail.web.backend.dto.NormeRequest;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.mapper.NormeMapper;
import portail.web.backend.exemple.portail.web.backend.repository.NormeRepository;
import portail.web.backend.exemple.portail.web.backend.service.NormeService;
import portail.web.backend.exemple.portail.web.backend.service.support.NormePdfStorage;
import portail.web.backend.exemple.portail.web.backend.service.support.NormeRelationResolver;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du NormeService.
 * Vérifie les règles : référence unique, norme existante pour update/delete,
 * nettoyage PDF lors de la suppression.
 */
@ExtendWith(MockitoExtension.class)
class NormeServiceTest {

    @Mock NormeRepository normeRepository;
    @Mock NormeRelationResolver normeRelationResolver;
    @Mock NormeMapper normeMapper;
    @Mock NormePdfStorage normePdfStorage;
    @InjectMocks NormeService service;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NormeRequest request(String reference) {
        return new NormeRequest(
                reference, null, "Titre FR", null, null,
                null, null, null,
                false, null, null, null,
                false, null,
                null, null, null, null, null, null, null, null, null, null
        );
    }

    private Norme normeWithPdf(Long id, String reference, String pdfPath) {
        Norme n = new Norme();
        n.setReference(reference);
        n.setPdfPath(pdfPath);
        return n;
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Test
    void create_referenceDupliquee_throwsBadRequest() {
        when(normeRepository.existsByReference("ISO-001")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.create(request("ISO-001")));
        verify(normeRepository, never()).save(any());
    }

    @Test
    void create_valid_appellesSave() {
        when(normeRepository.existsByReference("ISO-001")).thenReturn(false);
        when(normeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(request("ISO-001"));

        verify(normeRepository).save(any(Norme.class));
        verify(normeRelationResolver).apply(any(Norme.class), any(NormeRequest.class));
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    void update_normeIntrouvable_throwsResourceNotFound() {
        when(normeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request("ISO-001")));
    }

    @Test
    void update_referenceExistanteSurAutreNorme_throwsBadRequest() {
        Norme existing = normeWithPdf(1L, "ISO-OLD", null);
        when(normeRepository.findById(1L)).thenReturn(Optional.of(existing));
        // Nouvelle référence est déjà prise par une autre norme
        when(normeRepository.existsByReference("ISO-NEW")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.update(1L, request("ISO-NEW")));
        verify(normeRepository, never()).save(any());
    }

    @Test
    void update_memereference_pasDeConflitDeDoublon() {
        Norme existing = normeWithPdf(1L, "ISO-001", null);
        when(normeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(normeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // La même référence appartient déjà à cette norme → pas de conflit
        // existsByReference n'est pas appelé car norme.reference == request.reference

        assertDoesNotThrow(() -> service.update(1L, request("ISO-001")));
        verify(normeRepository).save(existing);
    }

    // ── delete() ─────────────────────────────────────────────────────────────

    @Test
    void delete_normeIntrouvable_throwsResourceNotFound() {
        when(normeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(normeRepository, never()).delete(any(Norme.class));
    }

    @Test
    void delete_valid_supprimeNormeEtNettoyePdf() {
        Norme norme = normeWithPdf(1L, "ISO-001", "norme-1-abc.pdf");
        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));

        service.delete(1L);

        verify(normeRepository).delete(norme);
        verify(normePdfStorage).deleteIfExists("norme-1-abc.pdf");
    }

    @Test
    void delete_sansPdf_appelleDeleteIfExistsSansErreur() {
        Norme norme = normeWithPdf(1L, "ISO-001", null); // pas de PDF
        when(normeRepository.findById(1L)).thenReturn(Optional.of(norme));

        assertDoesNotThrow(() -> service.delete(1L));

        verify(normeRepository).delete(norme);
        verify(normePdfStorage).deleteIfExists(null);
    }

    // ── findById() ───────────────────────────────────────────────────────────

    @Test
    void findById_notFound_throwsResourceNotFound() {
        when(normeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
