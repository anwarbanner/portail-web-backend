package portail.web.backend.exemple.portail.web.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.entity.IcsLevel3;

import java.util.List;
import java.util.Optional;

public interface IcsLevel3Repository extends JpaRepository<IcsLevel3, Long> {
    boolean existsByCode(String code);
    Optional<IcsLevel3> findByCode(String code);
    List<IcsLevel3> findByIcsLevel2Id(Long icsLevel2Id);

    @EntityGraph(attributePaths = "icsLevel2")
    Page<IcsLevel3> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "icsLevel2")
    Optional<IcsLevel3> findById(Long id);
}

