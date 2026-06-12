package portail.web.backend.exemple.portail.web.backend.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import portail.web.backend.exemple.portail.web.backend.dto.NormeImportReport;
import portail.web.backend.exemple.portail.web.backend.dto.NormeImportReport.NormeImportRowError;
import portail.web.backend.exemple.portail.web.backend.entity.*;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.repository.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class NormeExcelImportService {

    private final NormeRepository normeRepository;
    private final StatutRepository statutRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final NormeCollectionRepository normeCollectionRepository;
    private final IndustrialBranchRepository industrialBranchRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final SubFamilyRepository subFamilyRepository;
    private final Filter1Repository filter1Repository;
    private final IcsLevel1Repository icsLevel1Repository;
    private final IcsLevel2Repository icsLevel2Repository;
    private final IcsLevel3Repository icsLevel3Repository;

    public NormeExcelImportService(NormeRepository normeRepository,
                                   StatutRepository statutRepository,
                                   DocumentTypeRepository documentTypeRepository,
                                   NormeCollectionRepository normeCollectionRepository,
                                   IndustrialBranchRepository industrialBranchRepository,
                                   ProductFamilyRepository productFamilyRepository,
                                   SubFamilyRepository subFamilyRepository,
                                   Filter1Repository filter1Repository,
                                   IcsLevel1Repository icsLevel1Repository,
                                   IcsLevel2Repository icsLevel2Repository,
                                   IcsLevel3Repository icsLevel3Repository) {
        this.normeRepository = normeRepository;
        this.statutRepository = statutRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.normeCollectionRepository = normeCollectionRepository;
        this.industrialBranchRepository = industrialBranchRepository;
        this.productFamilyRepository = productFamilyRepository;
        this.subFamilyRepository = subFamilyRepository;
        this.filter1Repository = filter1Repository;
        this.icsLevel1Repository = icsLevel1Repository;
        this.icsLevel2Repository = icsLevel2Repository;
        this.icsLevel3Repository = icsLevel3Repository;
    }

    public enum OnDuplicate { SKIP, UPDATE }

    // Expected header names (case-insensitive, trimmed)
    private static final List<String> EXPECTED_HEADERS = List.of(
            "reference", "publicationdate", "titrefr", "titreen", "titrede",
            "descripteurfr", "descripteuren", "documentidentifier",
            "includedinsubscription", "afnorindex", "printnumber", "printdate",
            "mandatory", "regulationspecifique",
            "statutcode", "documenttypecode", "collectioncode",
            "industrialbranchcode", "productfamilycode", "subfamilycode",
            "filter1code", "icslevel1code", "icslevel2code", "icslevel3code"
    );

    @Transactional
    public NormeImportReport importFromExcel(MultipartFile file, OnDuplicate onDuplicate) {
        validateFile(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("Le fichier Excel est vide ou ne contient pas de ligne d'en-tête.");
            }

            Map<String, Integer> colIndex = buildColumnIndex(headerRow);
            validateRequiredHeaders(colIndex);

            int created = 0, updated = 0, skipped = 0;
            List<NormeImportRowError> errors = new ArrayList<>();

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                String reference = getString(row, colIndex, "reference");
                if (reference == null || reference.isBlank()) {
                    errors.add(new NormeImportRowError(i + 1, "", "Colonne 'reference' obligatoire manquante"));
                    continue;
                }

                try {
                    Optional<Norme> existing = normeRepository.findByReference(reference);
                    if (existing.isPresent()) {
                        if (onDuplicate == OnDuplicate.SKIP) {
                            skipped++;
                        } else {
                            applyRow(existing.get(), row, colIndex);
                            normeRepository.save(existing.get());
                            updated++;
                        }
                    } else {
                        Norme norme = new Norme();
                        norme.setReference(reference);
                        applyRow(norme, row, colIndex);
                        normeRepository.save(norme);
                        created++;
                    }
                } catch (Exception ex) {
                    errors.add(new NormeImportRowError(i + 1, reference, ex.getMessage()));
                }
            }

            int totalRows = lastRow;
            return new NormeImportReport(totalRows, created, updated, skipped, errors.size(), errors);

        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BadRequestException("Impossible de lire le fichier Excel: " + ex.getMessage());
        }
    }

    private void applyRow(Norme norme, Row row, Map<String, Integer> col) {
        norme.setPublicationDate(getDate(row, col, "publicationdate"));
        norme.setTitreFr(getString(row, col, "titrefr"));
        norme.setTitreEn(getString(row, col, "titreen"));
        norme.setTitreDe(getString(row, col, "titrede"));
        norme.setDescripteurFr(getString(row, col, "descripteurfr"));
        norme.setDescripteurEn(getString(row, col, "descripteuren"));
        norme.setDocumentIdentifier(getString(row, col, "documentidentifier"));
        norme.setAfnorIndex(getString(row, col, "afnorindex"));
        norme.setPrintNumber(getString(row, col, "printnumber"));
        norme.setPrintDate(getDate(row, col, "printdate"));
        norme.setRegulationSpecifique(getString(row, col, "regulationspecifique"));

        Boolean includedInSub = getBoolean(row, col, "includedinsubscription");
        norme.setIncludedInSubscription(includedInSub != null && includedInSub);

        Boolean mandatory = getBoolean(row, col, "mandatory");
        norme.setMandatory(mandatory != null && mandatory);

        norme.setStatut(resolveByCode(statutRepository::findByCode, getString(row, col, "statutcode")));
        norme.setDocumentType(resolveByCode(documentTypeRepository::findByCode, getString(row, col, "documenttypecode")));
        norme.setCollection(resolveByCode(normeCollectionRepository::findByCode, getString(row, col, "collectioncode")));
        norme.setIndustrialBranch(resolveByCode(industrialBranchRepository::findByCode, getString(row, col, "industrialbranchcode")));
        norme.setProductFamily(resolveByCode(productFamilyRepository::findByCode, getString(row, col, "productfamilycode")));
        norme.setSubFamily(resolveByCode(subFamilyRepository::findByCode, getString(row, col, "subfamilycode")));
        norme.setFilter1(resolveByCode(filter1Repository::findByCode, getString(row, col, "filter1code")));
        norme.setIcsLevel1(resolveByCode(icsLevel1Repository::findByCode, getString(row, col, "icslevel1code")));
        norme.setIcsLevel2(resolveByCode(icsLevel2Repository::findByCode, getString(row, col, "icslevel2code")));
        norme.setIcsLevel3(resolveByCode(icsLevel3Repository::findByCode, getString(row, col, "icslevel3code")));
    }

    private <T> T resolveByCode(java.util.function.Function<String, Optional<T>> finder, String code) {
        if (code == null || code.isBlank()) return null;
        return finder.apply(code.trim())
                .orElseThrow(() -> new BadRequestException("Code introuvable: '" + code + "'"));
    }

    private Map<String, Integer> buildColumnIndex(Row headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            String name = getCellStringValue(cell);
            if (name != null) {
                index.put(name.trim().toLowerCase().replaceAll("\\s+", ""), cell.getColumnIndex());
            }
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> colIndex) {
        if (!colIndex.containsKey("reference")) {
            throw new BadRequestException("Colonne obligatoire manquante dans l'en-tête: 'reference'");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("Seuls les fichiers .xlsx sont acceptés.");
        }
    }

    private String getString(Row row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        return getCellStringValue(cell);
    }

    private LocalDate getDate(Row row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String val = getCellStringValue(cell);
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Format de date invalide pour '" + key + "': '" + val + "' (attendu: yyyy-MM-dd)");
        }
    }

    private Boolean getBoolean(Row row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.BOOLEAN) return cell.getBooleanCellValue();
        String val = getCellStringValue(cell);
        if (val == null || val.isBlank()) return null;
        return "true".equalsIgnoreCase(val.trim()) || "oui".equalsIgnoreCase(val.trim()) || "1".equals(val.trim());
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellStringValue(cell);
                if (val != null && !val.isBlank()) return false;
            }
        }
        return true;
    }
}
