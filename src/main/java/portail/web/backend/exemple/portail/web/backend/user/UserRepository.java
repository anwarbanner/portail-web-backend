package portail.web.backend.exemple.portail.web.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseAndRole(String username, String role, Pageable pageable);
}
