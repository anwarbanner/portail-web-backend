package portail.web.backend.exemple.portail.web.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LookupRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 180) String name,
        @Size(max = 2000) String description,
        Long parentId
) {
}

