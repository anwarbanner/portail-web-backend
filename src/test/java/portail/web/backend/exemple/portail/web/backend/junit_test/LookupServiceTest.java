package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import portail.web.backend.exemple.portail.web.backend.dto.LookupRequest;
import portail.web.backend.exemple.portail.web.backend.dto.LookupResponse;
import portail.web.backend.exemple.portail.web.backend.entity.AbstractLookupEntity;
import portail.web.backend.exemple.portail.web.backend.entity.Statut;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.mapper.LookupMapper;
import portail.web.backend.exemple.portail.web.backend.service.LookupService;
import portail.web.backend.exemple.portail.web.backend.service.LookupType;
import portail.web.backend.exemple.portail.web.backend.service.support.LookupCatalog;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LookupServiceTest {

    @Mock LookupCatalog lookupCatalog;
    @Mock LookupMapper lookupMapper;
    @InjectMocks LookupService lookupService;

    @SuppressWarnings("unchecked")
    private JpaRepository<AbstractLookupEntity, Long> repo;

    private static final LookupType TYPE = LookupType.STATUT;
    private static final Pageable PAGE = PageRequest.of(0, 10);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repo = mock(JpaRepository.class);
    }

    private LookupResponse resp(Long id) {
        return new LookupResponse(id, "CODE", "Name", null, null, null, null, "STATUT");
    }

    private AbstractLookupEntity statut() {
        return new Statut();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsPageOfResponses() {
        AbstractLookupEntity entity = statut();
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.findAll(PAGE)).thenReturn(new PageImpl<>(List.of(entity)));
        when(lookupMapper.toResponse(entity, "STATUT")).thenReturn(resp(1L));

        Page<LookupResponse> result = lookupService.findAll(TYPE, PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_found_returnsResponse() {
        AbstractLookupEntity entity = statut();
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.findById(1L)).thenReturn(Optional.of(entity));
        when(lookupMapper.toResponse(entity, "STATUT")).thenReturn(resp(1L));

        LookupResponse result = lookupService.findById(TYPE, 1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookupService.findById(TYPE, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("STATUT not found with id=99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesEntityAndReturnsResponse() {
        LookupRequest request = new LookupRequest("CODE", "Name", null, null);
        AbstractLookupEntity entity = statut();
        when(lookupCatalog.normalizeCode(TYPE, "CODE", null)).thenReturn("CODE");
        when(lookupCatalog.newEntity(TYPE)).thenReturn(entity);
        when(lookupCatalog.attachParent(any(), eq(TYPE), isNull())).thenReturn(entity);
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.save(entity)).thenReturn(entity);
        when(lookupMapper.toResponse(entity, "STATUT")).thenReturn(resp(1L));

        LookupResponse result = lookupService.create(TYPE, request);

        assertThat(result.id()).isEqualTo(1L);
        verify(repo).save(entity);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_found_updatesAndReturnsResponse() {
        LookupRequest request = new LookupRequest("CODE2", "Name2", null, null);
        AbstractLookupEntity entity = statut();
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.findById(1L)).thenReturn(Optional.of(entity));
        when(lookupCatalog.normalizeCode(TYPE, "CODE2", null)).thenReturn("CODE2");
        when(lookupCatalog.attachParent(any(), eq(TYPE), isNull())).thenReturn(entity);
        when(repo.save(entity)).thenReturn(entity);
        when(lookupMapper.toResponse(entity, "STATUT")).thenReturn(resp(1L));

        LookupResponse result = lookupService.update(TYPE, 1L, request);

        assertThat(result.id()).isEqualTo(1L);
        verify(repo).save(entity);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        LookupRequest request = new LookupRequest("CODE", "Name", null, null);
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookupService.update(TYPE, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_found_callsDeleteById() {
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.existsById(1L)).thenReturn(true);

        lookupService.delete(TYPE, 1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(lookupCatalog.repository(TYPE)).thenReturn(repo);
        when(repo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> lookupService.delete(TYPE, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("STATUT not found with id=99");
    }
}
