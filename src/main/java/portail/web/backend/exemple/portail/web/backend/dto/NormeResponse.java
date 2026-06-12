package portail.web.backend.exemple.portail.web.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NormeResponse(
        Long id,
        String reference,
        LocalDate publicationDate,
        String titreFr,
        String titreEn,
        String titreDe,
        String descripteurFr,
        String descripteurEn,
        String documentIdentifier,
        boolean includedInSubscription,
        String afnorIndex,
        String printNumber,
        LocalDate printDate,
        boolean mandatory,
        String regulationSpecifique,
        boolean hasPdf,
        String pdfOriginalName,
        String pdfContentType,
        Long pdfSize,
        Long statutId,
        String statutCode,
        Long documentTypeId,
        String documentTypeCode,
        Long collectionId,
        String collectionCode,
        Long industrialBranchId,
        String industrialBranchCode,
        Long productFamilyId,
        String productFamilyCode,
        Long subFamilyId,
        String subFamilyCode,
        Long filter1Id,
        String filter1Code,
        Long icsLevel1Id,
        String icsLevel1Code,
        Long icsLevel2Id,
        String icsLevel2Code,
        Long icsLevel3Id,
        String icsLevel3Code,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

