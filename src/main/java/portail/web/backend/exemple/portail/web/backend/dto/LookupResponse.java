package portail.web.backend.exemple.portail.web.backend.dto;

public record LookupResponse(
        Long id,
        String code,
        String name,
        String description,
        Long parentId,
        String parentCode,
        String parentName,
        String type
) {
}
