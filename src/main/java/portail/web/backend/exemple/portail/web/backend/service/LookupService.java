package portail.web.backend.exemple.portail.web.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import portail.web.backend.exemple.portail.web.backend.dto.LookupRequest;
import portail.web.backend.exemple.portail.web.backend.dto.LookupResponse;
import portail.web.backend.exemple.portail.web.backend.entity.AbstractLookupEntity;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.mapper.LookupMapper;
import portail.web.backend.exemple.portail.web.backend.service.support.LookupCatalog;

@Service
public class LookupService {

    private final LookupCatalog lookupCatalog;
    private final LookupMapper lookupMapper;

    public LookupService(LookupCatalog lookupCatalog,
                         LookupMapper lookupMapper) {
        this.lookupCatalog = lookupCatalog;
        this.lookupMapper = lookupMapper;
    }

    public Page<LookupResponse> findAll(LookupType type, Pageable pageable) {
        return lookupCatalog.repository(type).findAll(pageable)
                .map(entity -> lookupMapper.toResponse(entity, type.name()));
    }

    public LookupResponse findById(LookupType type, Long id) {
        AbstractLookupEntity entity = lookupCatalog.repository(type).findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(type + " not found with id=" + id));
        return lookupMapper.toResponse(entity, type.name());
    }

    public LookupResponse create(LookupType type, LookupRequest request) {
        String normalizedCode = lookupCatalog.normalizeCode(type, request.code(), request.parentId());
        AbstractLookupEntity entity = lookupCatalog.attachParent(
                applyCommonFields(lookupCatalog.newEntity(type), request, normalizedCode),
                type,
                request.parentId()
        );
        return lookupMapper.toResponse(lookupCatalog.repository(type).save(entity), type.name());
    }

    public LookupResponse update(LookupType type, Long id, LookupRequest request) {
        AbstractLookupEntity existing = lookupCatalog.repository(type).findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(type + " not found with id=" + id));
        String normalizedCode = lookupCatalog.normalizeCode(type, request.code(), request.parentId());
        lookupCatalog.attachParent(applyCommonFields(existing, request, normalizedCode), type, request.parentId());
        return lookupMapper.toResponse(lookupCatalog.repository(type).save(existing), type.name());
    }

    public void delete(LookupType type, Long id) {
        if (!lookupCatalog.repository(type).existsById(id)) {
            throw new ResourceNotFoundException(type + " not found with id=" + id);
        }
        lookupCatalog.repository(type).deleteById(id);
    }

    private AbstractLookupEntity applyCommonFields(AbstractLookupEntity entity, LookupRequest request, String normalizedCode) {
        entity.setCode(normalizedCode);
        entity.setName(request.name());
        entity.setDescription(request.description());
        return entity;
    }
}
