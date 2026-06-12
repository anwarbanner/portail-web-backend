package portail.web.backend.exemple.portail.web.backend.consultation.dto;

import java.time.LocalDateTime;

public record ConsultationResponse(
        Long id,
        Long normeId,
        String normeReference,
        String normeTitreFr,
        LocalDateTime dateConsultation
) {
}
