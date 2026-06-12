package portail.web.backend.exemple.portail.web.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Size(min = 0, max = 100) String password,
        @NotBlank String role
) {
}
