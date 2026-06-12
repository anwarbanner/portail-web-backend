package portail.web.backend.exemple.portail.web.backend.service.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import portail.web.backend.exemple.portail.web.backend.entity.*;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.repository.*;
import portail.web.backend.exemple.portail.web.backend.service.LookupType;

import java.util.Objects;

@Component
public class LookupCatalog {

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

    public LookupCatalog(StatutRepository statutRepository,
                         DocumentTypeRepository documentTypeRepository,
                         NormeCollectionRepository normeCollectionRepository,
                         IndustrialBranchRepository industrialBranchRepository,
                         ProductFamilyRepository productFamilyRepository,
                         SubFamilyRepository subFamilyRepository,
                         Filter1Repository filter1Repository,
                         IcsLevel1Repository icsLevel1Repository,
                         IcsLevel2Repository icsLevel2Repository,
                         IcsLevel3Repository icsLevel3Repository) {
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

    @SuppressWarnings("unchecked")
    public JpaRepository<AbstractLookupEntity, Long> repository(LookupType type) {
        return (JpaRepository<AbstractLookupEntity, Long>) switch (type) {
            case STATUT -> statutRepository;
            case DOCUMENT_TYPE -> documentTypeRepository;
            case COLLECTION -> normeCollectionRepository;
            case INDUSTRIAL_BRANCH -> industrialBranchRepository;
            case PRODUCT_FAMILY -> productFamilyRepository;
            case SUB_FAMILY -> subFamilyRepository;
            case FILTER1 -> filter1Repository;
            case ICS_LEVEL1 -> icsLevel1Repository;
            case ICS_LEVEL2 -> icsLevel2Repository;
            case ICS_LEVEL3 -> icsLevel3Repository;
        };
    }

    public AbstractLookupEntity newEntity(LookupType type) {
        return switch (type) {
            case STATUT -> new Statut();
            case DOCUMENT_TYPE -> new DocumentType();
            case COLLECTION -> new NormeCollection();
            case INDUSTRIAL_BRANCH -> new IndustrialBranch();
            case PRODUCT_FAMILY -> new ProductFamily();
            case SUB_FAMILY -> new SubFamily();
            case FILTER1 -> new Filter1();
            case ICS_LEVEL1 -> new IcsLevel1();
            case ICS_LEVEL2 -> new IcsLevel2();
            case ICS_LEVEL3 -> new IcsLevel3();
        };
    }

    public AbstractLookupEntity attachParent(AbstractLookupEntity entity, LookupType type, Long parentId) {
        if (type == LookupType.ICS_LEVEL2) {
            if (Objects.isNull(parentId)) {
                throw new BadRequestException("parentId is required for ICS_LEVEL2");
            }
            IcsLevel1 parent = icsLevel1Repository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("IcsLevel1 not found with id=" + parentId));
            ((IcsLevel2) entity).setIcsLevel1(parent);
        }

        if (type == LookupType.ICS_LEVEL3) {
            if (Objects.isNull(parentId)) {
                throw new BadRequestException("parentId is required for ICS_LEVEL3");
            }
            IcsLevel2 parent = icsLevel2Repository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("IcsLevel2 not found with id=" + parentId));
            ((IcsLevel3) entity).setIcsLevel2(parent);
        }

        return entity;
    }

    public String normalizeCode(LookupType type, String code, Long parentId) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("code is required");
        }

        if (type == LookupType.ICS_LEVEL2) {
            if (Objects.isNull(parentId)) {
                throw new BadRequestException("parentId is required for ICS_LEVEL2");
            }
            IcsLevel1 parent = icsLevel1Repository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("IcsLevel1 not found with id=" + parentId));
            return normalizeIcsCode(parent.getCode(), code, 2, "ICS_LEVEL2");
        }

        if (type == LookupType.ICS_LEVEL3) {
            if (Objects.isNull(parentId)) {
                throw new BadRequestException("parentId is required for ICS_LEVEL3");
            }
            IcsLevel2 parent = icsLevel2Repository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("IcsLevel2 not found with id=" + parentId));
            return normalizeIcsCode(parent.getCode(), code, 3, "ICS_LEVEL3");
        }

        return code;
    }

    private String normalizeIcsCode(String parentCode, String code, int segmentLength, String type) {
        String trimmed = code.trim();
        String prefix = parentCode + ".";
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }

        if (trimmed.contains(".")) {
            throw new BadRequestException(type + " code must be a suffix like '01' or full code like '" + prefix + "01'");
        }

        String segment = padNumericSegment(trimmed, segmentLength);
        return prefix + segment;
    }

    private String padNumericSegment(String value, int length) {
        if (!value.matches("\\d+")) {
            return value;
        }
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
    }
}
