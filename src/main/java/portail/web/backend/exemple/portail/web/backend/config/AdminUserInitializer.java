package portail.web.backend.exemple.portail.web.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

@Component
@ConditionalOnProperty(name = "app.admin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                org.springframework.core.env.Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = environment.getProperty("app.admin.username", "admin");
        this.adminPassword = environment.getProperty("app.admin.password", "Admin@12345");
    }

    @Override
    public void run(String... args) {
        var existing = userRepository.findByUsername(adminUsername);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setPassword(passwordEncoder.encode(adminPassword));
            user.setRole("ROLE_ADMIN");
            userRepository.save(user);
            log.info("Ensured admin user '{}' has ROLE_ADMIN", adminUsername);
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ROLE_ADMIN");
        userRepository.save(admin);

        log.info("Seeded default admin user '{}' with role ROLE_ADMIN", adminUsername);
    }
}



