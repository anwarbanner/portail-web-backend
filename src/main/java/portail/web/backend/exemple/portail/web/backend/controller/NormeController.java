package portail.web.backend.exemple.portail.web.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import portail.web.backend.exemple.portail.web.backend.dto.NormeImportReport;
import portail.web.backend.exemple.portail.web.backend.dto.NormeRequest;
import portail.web.backend.exemple.portail.web.backend.dto.NormeResponse;
import portail.web.backend.exemple.portail.web.backend.service.NormeExcelImportService;
import portail.web.backend.exemple.portail.web.backend.service.NormeExcelImportService.OnDuplicate;
import portail.web.backend.exemple.portail.web.backend.service.NormePdfDownload;
import portail.web.backend.exemple.portail.web.backend.service.NormeService;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementGuard;

@RestController
@RequestMapping("/api/normes")
public class NormeController {

    private final NormeService normeService;
    private final AbonnementGuard abonnementGuard;
    private final NormeExcelImportService normeExcelImportService;

    public NormeController(NormeService normeService,
                           AbonnementGuard abonnementGuard,
                           NormeExcelImportService normeExcelImportService) {
        this.normeService = normeService;
        this.abonnementGuard = abonnementGuard;
        this.normeExcelImportService = normeExcelImportService;
    }

    @Operation(summary = "List normes with pagination, search and filters")
    @GetMapping
    public Page<NormeResponse> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long statutId,
            @RequestParam(required = false) Long icsLevel1Id,
            @RequestParam(required = false) Long icsLevel2Id,
            @RequestParam(required = false) Long icsLevel3Id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return normeService.findAll(search, reference, statutId, icsLevel1Id, icsLevel2Id, icsLevel3Id, pageable);
    }

    @Operation(summary = "Get norme by id")
    @GetMapping("/{id}")
    public NormeResponse findById(@PathVariable Long id) {
        return normeService.findById(id);
    }

    @Operation(summary = "Create norme")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NormeResponse> create(@Valid @RequestBody NormeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(normeService.create(request));
    }

    @Operation(summary = "Update norme")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NormeResponse update(@PathVariable Long id, @Valid @RequestBody NormeRequest request) {
        return normeService.update(id, request);
    }

    @Operation(summary = "Upload norme PDF")
    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public NormeResponse uploadPdf(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return normeService.uploadPdf(id, file);
    }

    @Operation(summary = "Download norme PDF")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id, Authentication authentication) {
        abonnementGuard.verifierEtEnregistrer(authentication, id);
        NormePdfDownload pdf = normeService.downloadPdf(id);
        MediaType mediaType = pdf.contentType() == null
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType(pdf.contentType());
        String filename = pdf.filename() == null ? "norme.pdf" : pdf.filename();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString());

        if (pdf.size() != null) {
            builder.contentLength(pdf.size());
        }

        return builder.body(pdf.resource());
    }

    @Operation(summary = "Delete norme")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        normeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Import normes from Excel (.xlsx)")
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public NormeImportReport importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "onDuplicate", defaultValue = "SKIP") OnDuplicate onDuplicate
    ) {
        return normeExcelImportService.importFromExcel(file, onDuplicate);
    }
}

