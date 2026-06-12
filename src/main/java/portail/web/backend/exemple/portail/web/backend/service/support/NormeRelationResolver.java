package portail.web.backend.exemple.portail.web.backend.service.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import portail.web.backend.exemple.portail.web.backend.dto.NormeRequest;
import portail.web.backend.exemple.portail.web.backend.entity.*;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.repository.*;

@Component
public class NormeRelationResolver {

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

    public NormeRelationResolver(StatutRepository statutRepository,
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

    public void apply(Norme norme, NormeRequest request) {
        norme.setStatut(load(statutRepository, request.statutId()));
        norme.setDocumentType(load(documentTypeRepository, request.documentTypeId()));
        norme.setCollection(load(normeCollectionRepository, request.collectionId()));
        norme.setIndustrialBranch(load(industrialBranchRepository, request.industrialBranchId()));
        norme.setProductFamily(load(productFamilyRepository, request.productFamilyId()));
        norme.setSubFamily(load(subFamilyRepository, request.subFamilyId()));
        norme.setFilter1(load(filter1Repository, request.filter1Id()));
        norme.setIcsLevel1(load(icsLevel1Repository, request.icsLevel1Id()));
        norme.setIcsLevel2(load(icsLevel2Repository, request.icsLevel2Id()));
        norme.setIcsLevel3(load(icsLevel3Repository, request.icsLevel3Id()));
    }

    private <T> T load(JpaRepository<T, Long> repository, Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Related entity not found with id=" + id));
    }
}
