package portail.web.backend.exemple.portail.web.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.user.UserService;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserRequest;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserResponse;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserUpdateRequest;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * List all users with optional filtering by username and role
     */
    @Operation(summary = "List all users")
    @GetMapping
    public Page<UserResponse> findAll(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return userService.findAll(username, role, pageable);
    }

    /**
     * Get user by ID
     */
    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * Create new user
     */
    @Operation(summary = "Create new user")
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update user
     */
    @Operation(summary = "Update user")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /**
     * Delete user
     */
    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

