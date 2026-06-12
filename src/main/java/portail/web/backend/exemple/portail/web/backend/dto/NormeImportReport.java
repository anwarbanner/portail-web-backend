package portail.web.backend.exemple.portail.web.backend.dto;

import java.util.List;

public record NormeImportReport(
        int totalRows,
        int created,
        int updated,
        int skipped,
        int errors,
        List<NormeImportRowError> rowErrors
) {
    public record NormeImportRowError(int row, String reference, String message) {}
}
