package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import portail.web.backend.exemple.portail.web.backend.entity.Norme;

import java.util.Optional;

public interface NormeRepository extends JpaRepository<Norme, Long>, JpaSpecificationExecutor<Norme> {
    boolean existsByReference(String reference);
    Optional<Norme> findByReference(String reference);

    @EntityGraph(attributePaths = {
        "statut", "documentType", "collection", "industrialBranch",
        "productFamily", "subFamily", "filter1",
        "icsLevel1", "icsLevel2", "icsLevel3"
    })
    Page<Norme> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
        "statut", "documentType", "collection", "industrialBranch",
        "productFamily", "subFamily", "filter1",
        "icsLevel1", "icsLevel2", "icsLevel3"
    })
    Optional<Norme> findById(Long id);

    @EntityGraph(attributePaths = {
        "statut", "documentType", "collection", "industrialBranch",
        "productFamily", "subFamily", "filter1",
        "icsLevel1", "icsLevel2", "icsLevel3"
    })
    Page<Norme> findAll(Specification<Norme> spec, Pageable pageable);
}

