package portail.web.backend.exemple.portail.web.backend.mapper;

import org.springframework.stereotype.Component;
import portail.web.backend.exemple.portail.web.backend.dto.LookupResponse;
import portail.web.backend.exemple.portail.web.backend.entity.AbstractLookupEntity;
import portail.web.backend.exemple.portail.web.backend.entity.IcsLevel2;
import portail.web.backend.exemple.portail.web.backend.entity.IcsLevel3;

@Component
public class LookupMapper {

    public LookupResponse toResponse(AbstractLookupEntity entity, String type) {
        Long parentId = null;
        String parentCode = null;
        String parentName = null;
        if (entity instanceof IcsLevel2 icsLevel2) {
            if (icsLevel2.getIcsLevel1() != null) {
                parentId = icsLevel2.getIcsLevel1().getId();
                parentCode = icsLevel2.getIcsLevel1().getCode();
                parentName = icsLevel2.getIcsLevel1().getName();
            }
        } else if (entity instanceof IcsLevel3 icsLevel3) {
            if (icsLevel3.getIcsLevel2() != null) {
                parentId = icsLevel3.getIcsLevel2().getId();
                parentCode = icsLevel3.getIcsLevel2().getCode();
                parentName = icsLevel3.getIcsLevel2().getName();
            }
        }

        return new LookupResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                parentId,
                parentCode,
                parentName,
                type
        );
    }
}
