package portail.web.backend.exemple.portail.web.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Users", description = "User administration endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users")
    @GetMapping
    public Page<UserResponse> listUsers(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        return userService.findAll(username, role, pageable);
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Create user")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "Update user by id")
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Delete user by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
