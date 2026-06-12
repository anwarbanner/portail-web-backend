package portail.web.backend.exemple.portail.web.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserRequest;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserResponse;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserUpdateRequest;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Get all users with optional filtering by username and role
     */
    public Page<UserResponse> findAll(String username, String role, Pageable pageable) {
        Page<User> users;

        if (username != null && !username.isBlank() && role != null && !role.isBlank()) {
            users = userRepository.findByUsernameContainingIgnoreCaseAndRole(username, role, pageable);
        } else if (username != null && !username.isBlank()) {
            users = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (role != null && !role.isBlank()) {
            users = userRepository.findByRole(role, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(user -> new UserResponse(user.getId(), user.getUsername(), user.getRole()));
    }

    /**
     * Get user by ID
     */
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    /**
     * Create new user
     */
    public UserResponse create(UserRequest request) {
        // Validate username not taken
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username '" + request.username() + "' is already taken");
        }

        // Validate role
        if (!isValidRole(request.role())) {
            throw new BadRequestException("Invalid role: " + request.role() + ". Expected ROLE_ADMIN or ROLE_USER");
        }

        // Create and save user
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role()));

        User savedUser = userRepository.save(user);
        return new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }

    /**
     * Update user
     */
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if username is already taken by another user
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username '" + request.username() + "' is already taken");
        }

        // Validate role
        if (!isValidRole(request.role())) {
            throw new BadRequestException("Invalid role: " + request.role() + ". Expected ROLE_ADMIN or ROLE_USER");
        }

        // Update fields
        user.setUsername(request.username());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        user.setRole(normalizeRole(request.role()));

        User updatedUser = userRepository.save(user);
        return new UserResponse(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getRole());
    }

    /**
     * Delete user
     */
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    /**
     * Normalize role to ensure ROLE_ prefix
     */
    private String normalizeRole(String role) {
        if (role == null) {
            return "ROLE_USER";
        }
        String normalized = role.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        return normalized;
    }

    /**
     * Validate role format
     */
    private boolean isValidRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = normalizeRole(role);
        return normalized.equals("ROLE_ADMIN") || normalized.equals("ROLE_USER");
    }
}

