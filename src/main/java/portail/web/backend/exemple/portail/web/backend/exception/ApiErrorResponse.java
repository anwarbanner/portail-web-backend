package portail.web.backend.exemple.portail.web.backend.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        List<String> details
) {
}

