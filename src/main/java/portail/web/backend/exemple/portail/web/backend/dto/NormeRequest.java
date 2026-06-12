package portail.web.backend.exemple.portail.web.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NormeRequest(
        @NotBlank @Size(max = 120) String reference,
        LocalDate publicationDate,
        @Size(max = 255) String titreFr,
        @Size(max = 255) String titreEn,
        @Size(max = 255) String titreDe,
        String descripteurFr,
        String descripteurEn,
        @Size(max = 120) String documentIdentifier,
        Boolean includedInSubscription,
        @Size(max = 80) String afnorIndex,
        @Size(max = 80) String printNumber,
        LocalDate printDate,
        Boolean mandatory,
        String regulationSpecifique,
        Long statutId,
        Long documentTypeId,
        Long collectionId,
        Long industrialBranchId,
        Long productFamilyId,
        Long subFamilyId,
        Long filter1Id,
        Long icsLevel1Id,
        Long icsLevel2Id,
        Long icsLevel3Id
) {
}

