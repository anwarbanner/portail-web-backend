package portail.web.backend.exemple.portail.web.backend.mapper;

import org.springframework.stereotype.Component;
import portail.web.backend.exemple.portail.web.backend.dto.NormeResponse;
import portail.web.backend.exemple.portail.web.backend.entity.AbstractLookupEntity;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;

@Component
public class NormeMapper {

    public NormeResponse toResponse(Norme norme) {
        return new NormeResponse(
                norme.getId(),
                norme.getReference(),
                norme.getPublicationDate(),
                norme.getTitreFr(),
                norme.getTitreEn(),
                norme.getTitreDe(),
                norme.getDescripteurFr(),
                norme.getDescripteurEn(),
                norme.getDocumentIdentifier(),
                norme.isIncludedInSubscription(),
                norme.getAfnorIndex(),
                norme.getPrintNumber(),
                norme.getPrintDate(),
                norme.isMandatory(),
                norme.getRegulationSpecifique(),
                norme.getPdfPath() != null,
                norme.getPdfOriginalName(),
                norme.getPdfContentType(),
                norme.getPdfSize(),
                idOf(norme.getStatut()), codeOf(norme.getStatut()),
                idOf(norme.getDocumentType()), codeOf(norme.getDocumentType()),
                idOf(norme.getCollection()), codeOf(norme.getCollection()),
                idOf(norme.getIndustrialBranch()), codeOf(norme.getIndustrialBranch()),
                idOf(norme.getProductFamily()), codeOf(norme.getProductFamily()),
                idOf(norme.getSubFamily()), codeOf(norme.getSubFamily()),
                idOf(norme.getFilter1()), codeOf(norme.getFilter1()),
                idOf(norme.getIcsLevel1()), codeOf(norme.getIcsLevel1()),
                idOf(norme.getIcsLevel2()), codeOf(norme.getIcsLevel2()),
                idOf(norme.getIcsLevel3()), codeOf(norme.getIcsLevel3()),
                norme.getCreatedAt(),
                norme.getUpdatedAt()
        );
    }

    private Long idOf(AbstractLookupEntity entity) {
        return entity == null ? null : entity.getId();
    }

    private String codeOf(AbstractLookupEntity entity) {
        return entity == null ? null : entity.getCode();
    }
}

