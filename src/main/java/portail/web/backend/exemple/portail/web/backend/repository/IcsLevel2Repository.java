package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.IcsLevel2;

import java.util.List;
import java.util.Optional;

public interface IcsLevel2Repository extends JpaRepository<IcsLevel2, Long> {
    boolean existsByCode(String code);
    Optional<IcsLevel2> findByCode(String code);
    List<IcsLevel2> findByIcsLevel1Id(Long icsLevel1Id);
    
    @EntityGraph(attributePaths = "icsLevel1")
    Page<IcsLevel2> findAll(Pageable pageable);
    
    @EntityGraph(attributePaths = "icsLevel1")
    Optional<IcsLevel2> findById(Long id);
}

