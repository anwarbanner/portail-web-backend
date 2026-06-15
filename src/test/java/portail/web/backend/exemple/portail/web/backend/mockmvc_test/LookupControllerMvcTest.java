package portail.web.backend.exemple.portail.web.backend.mockmvc_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.controller.LookupController;
import portail.web.backend.exemple.portail.web.backend.dto.LookupRequest;
import portail.web.backend.exemple.portail.web.backend.dto.LookupResponse;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.service.LookupService;
import portail.web.backend.exemple.portail.web.backend.service.LookupType;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LookupControllerMvcTest {

    @Mock LookupService lookupService;
    @InjectMocks LookupController lookupController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String BASE = "/api/lookups/STATUT";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders
                .standaloneSetup(lookupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private LookupResponse response(Long id) {
        return new LookupResponse(id, "STAT_01", "Statut actif", null, null, null, null, "STATUT");
    }

    // ── GET /api/lookups/{type} ───────────────────────────────────────────────

    @Test
    void findAll_returns200WithPage() throws Exception {
        when(lookupService.findAll(eq(LookupType.STATUT), any()))
                .thenReturn(new PageImpl<>(List.of(response(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].code").value("STAT_01"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── GET /api/lookups/{type}/{id} ──────────────────────────────────────────

    @Test
    void findById_found_returns200() throws Exception {
        when(lookupService.findById(LookupType.STATUT, 1L)).thenReturn(response(1L));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("STAT_01"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        when(lookupService.findById(LookupType.STATUT, 99L))
                .thenThrow(new ResourceNotFoundException("STATUT not found with id=99"));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("STATUT not found with id=99"));
    }

    // ── POST /api/lookups/{type} ──────────────────────────────────────────────

    @Test
    void create_validRequest_returns201() throws Exception {
        LookupRequest request = new LookupRequest("STAT_02", "Nouveau statut", null, null);
        when(lookupService.create(eq(LookupType.STATUT), any())).thenReturn(response(2L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    // ── PUT /api/lookups/{type}/{id} ──────────────────────────────────────────

    @Test
    void update_found_returns200() throws Exception {
        LookupRequest request = new LookupRequest("STAT_01_UPD", "Statut mis à jour", null, null);
        when(lookupService.update(eq(LookupType.STATUT), eq(1L), any())).thenReturn(response(1L));

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        LookupRequest request = new LookupRequest("CODE", "Name", null, null);
        when(lookupService.update(eq(LookupType.STATUT), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("STATUT not found with id=99"));

        mockMvc.perform(put(BASE + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/lookups/{type}/{id} ───────────────────────────────────────

    @Test
    void delete_found_returns204() throws Exception {
        doNothing().when(lookupService).delete(LookupType.STATUT, 1L);

        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());

        verify(lookupService).delete(LookupType.STATUT, 1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("STATUT not found with id=99"))
                .when(lookupService).delete(LookupType.STATUT, 99L);

        mockMvc.perform(delete(BASE + "/99"))
                .andExpect(status().isNotFound());
    }
}
