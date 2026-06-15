package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import portail.web.backend.exemple.portail.web.backend.dto.NormeImportReport;
import portail.web.backend.exemple.portail.web.backend.entity.*;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.repository.*;
import portail.web.backend.exemple.portail.web.backend.service.NormeExcelImportService;
import portail.web.backend.exemple.portail.web.backend.service.NormeExcelImportService.OnDuplicate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du NormeExcelImportService.
 * Vérifie la validation du fichier, le parsing des colonnes,
 * la gestion des doublons (SKIP/UPDATE) et l'accumulation des erreurs par ligne.
 */
@ExtendWith(MockitoExtension.class)
class NormeExcelImportServiceTest {

    @Mock NormeRepository normeRepository;
    @Mock StatutRepository statutRepository;
    @Mock DocumentTypeRepository documentTypeRepository;
    @Mock NormeCollectionRepository normeCollectionRepository;
    @Mock IndustrialBranchRepository industrialBranchRepository;
    @Mock ProductFamilyRepository productFamilyRepository;
    @Mock SubFamilyRepository subFamilyRepository;
    @Mock Filter1Repository filter1Repository;
    @Mock IcsLevel1Repository icsLevel1Repository;
    @Mock IcsLevel2Repository icsLevel2Repository;
    @Mock IcsLevel3Repository icsLevel3Repository;

    @InjectMocks NormeExcelImportService service;

    // ── Builders XLSX en mémoire ──────────────────────────────────────────────

    /** Crée un fichier .xlsx en mémoire avec header + lignes de données. */
    private MockMultipartFile xlsx(String[] headers, String[]... rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                Row dataRow = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) {
                        dataRow.createCell(c).setCellValue(rows[r][c]);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile(
                    "file", "normes.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private static final String[] MINIMAL_HEADERS = {"reference"};
    private static final String[] FULL_HEADERS = {
            "reference", "publicationDate", "titreFr", "titreEn", "titreDe",
            "descripteurFr", "descripteurEn", "documentIdentifier",
            "includedInSubscription", "afnorIndex", "printNumber", "printDate",
            "mandatory", "regulationSpecifique",
            "statutCode", "documentTypeCode", "collectionCode",
            "industrialBranchCode", "productFamilyCode", "subFamilyCode",
            "filter1Code", "icsLevel1Code", "icsLevel2Code", "icsLevel3Code"
    };

    // ── Validation du fichier ─────────────────────────────────────────────────

    @Test
    void fichierNull_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> service.importFromExcel(null, OnDuplicate.SKIP));
    }

    @Test
    void fichierVide_throwsBadRequest() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "normes.xlsx", "application/xlsx", new byte[0]);
        assertThrows(BadRequestException.class,
                () -> service.importFromExcel(empty, OnDuplicate.SKIP));
    }

    @Test
    void fichierCsv_throwsBadRequest() {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "normes.csv", "text/csv", "reference\nISO-001".getBytes());
        assertThrows(BadRequestException.class,
                () -> service.importFromExcel(csv, OnDuplicate.SKIP));
    }

    @Test
    void sansCOlonneReference_throwsBadRequest() throws IOException {
        // Header sans colonne "reference"
        MockMultipartFile file = xlsx(new String[]{"titreFr", "titreDe"}, new String[]{"Titre", "DE"});
        assertThrows(BadRequestException.class,
                () -> service.importFromExcel(file, OnDuplicate.SKIP));
    }

    // ── Création ──────────────────────────────────────────────────────────────

    @Test
    void ligneValide_normeInexistante_created() throws IOException {
        MockMultipartFile file = xlsx(MINIMAL_HEADERS, new String[]{"ISO-001"});
        when(normeRepository.findByReference("ISO-001")).thenReturn(Optional.empty());
        when(normeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(1, report.created());
        assertEquals(0, report.errors());
        assertEquals(0, report.skipped());
        verify(normeRepository).save(any());
    }

    @Test
    void referenceVideDansLigne_rowError() throws IOException {
        // La ligne doit avoir au moins un champ non-vide pour ne pas être détectée comme vide
        // On met titreFr renseigné mais reference="" → le service doit signaler l'erreur
        MockMultipartFile file = xlsx(
                new String[]{"reference", "titreFr"},
                new String[]{"", "Norme sans référence"});

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(0, report.created());
        assertEquals(1, report.errors());
        assertFalse(report.rowErrors().isEmpty());
        verify(normeRepository, never()).save(any());
    }

    // ── Gestion des doublons ─────────────────────────────────────────────────

    @Test
    void referenceDupliquee_skip_skipped() throws IOException {
        MockMultipartFile file = xlsx(MINIMAL_HEADERS, new String[]{"ISO-001"});
        Norme existing = new Norme();
        existing.setReference("ISO-001");
        when(normeRepository.findByReference("ISO-001")).thenReturn(Optional.of(existing));

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(0, report.created());
        assertEquals(1, report.skipped());
        assertEquals(0, report.errors());
        verify(normeRepository, never()).save(any());
    }

    @Test
    void referenceDupliquee_update_updated() throws IOException {
        MockMultipartFile file = xlsx(MINIMAL_HEADERS, new String[]{"ISO-001"});
        Norme existing = new Norme();
        existing.setReference("ISO-001");
        when(normeRepository.findByReference("ISO-001")).thenReturn(Optional.of(existing));
        when(normeRepository.save(existing)).thenReturn(existing);

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.UPDATE);

        assertEquals(0, report.created());
        assertEquals(1, report.updated());
        assertEquals(0, report.errors());
        verify(normeRepository).save(existing);
    }

    // ── Erreurs de données ────────────────────────────────────────────────────

    @Test
    void codeStatutInvalide_rowError() throws IOException {
        MockMultipartFile file = xlsx(FULL_HEADERS,
                new String[]{"ISO-001", null, "Titre", null, null, null, null, null,
                        null, null, null, null, null, null,
                        "CODE_INEXISTANT", null, null, null, null, null, null, null, null, null});

        when(normeRepository.findByReference("ISO-001")).thenReturn(Optional.empty());
        when(statutRepository.findByCode("CODE_INEXISTANT")).thenReturn(Optional.empty());

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(0, report.created());
        assertEquals(1, report.errors());
        assertTrue(report.rowErrors().get(0).message().contains("CODE_INEXISTANT"));
    }

    @Test
    void dateFormatInvalide_rowError() throws IOException {
        // publicationDate avec format incorrect
        MockMultipartFile file = xlsx(FULL_HEADERS,
                new String[]{"ISO-002", "32/13/2026", "Titre", null, null, null, null, null,
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null});

        when(normeRepository.findByReference("ISO-002")).thenReturn(Optional.empty());

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(1, report.errors());
    }

    // ── Parsing booléens ─────────────────────────────────────────────────────

    @Test
    void booleanOui_interpretaCommeTrue() throws IOException {
        // includedInSubscription = "oui" → true
        MockMultipartFile file = xlsx(
                new String[]{"reference", "includedInSubscription"},
                new String[]{"ISO-003", "oui"});

        when(normeRepository.findByReference("ISO-003")).thenReturn(Optional.empty());
        when(normeRepository.save(any())).thenAnswer(inv -> {
            Norme n = inv.getArgument(0);
            assertTrue(n.isIncludedInSubscription());
            return n;
        });

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);
        assertEquals(1, report.created());
    }

    // ── Rapport global ────────────────────────────────────────────────────────

    @Test
    void rapportTotalRows_correspondAuNombreDeLignesDonnees() throws IOException {
        MockMultipartFile file = xlsx(MINIMAL_HEADERS,
                new String[]{"ISO-A"},
                new String[]{"ISO-B"},
                new String[]{"ISO-C"});

        when(normeRepository.findByReference(anyString())).thenReturn(Optional.empty());
        when(normeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NormeImportReport report = service.importFromExcel(file, OnDuplicate.SKIP);

        assertEquals(3, report.created());
        assertEquals(0, report.errors());
    }
}
