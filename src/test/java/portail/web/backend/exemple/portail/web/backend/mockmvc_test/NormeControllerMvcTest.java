package portail.web.backend.exemple.portail.web.backend.mockmvc_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import portail.web.backend.exemple.portail.web.backend.abonnement.service.AbonnementGuard;
import portail.web.backend.exemple.portail.web.backend.controller.NormeController;
import portail.web.backend.exemple.portail.web.backend.dto.NormeImportReport;
import portail.web.backend.exemple.portail.web.backend.dto.NormeResponse;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.exception.SubscriptionRequiredException;
import portail.web.backend.exemple.portail.web.backend.service.NormeExcelImportService;
import portail.web.backend.exemple.portail.web.backend.service.NormeService;
import portail.web.backend.exemple.portail.web.backend.service.NormePdfDownload;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests MockMvc du NormeController — approche standalone (Spring Boot 4.x).
 * Vérifie le routage, la sérialisation JSON, la validation et le mapping des exceptions.
 */
@ExtendWith(MockitoExtension.class)
class
NormeControllerMvcTest {

    @Mock NormeService normeService;
    @Mock AbonnementGuard abonnementGuard;
    @Mock NormeExcelImportService normeExcelImportService;

    @InjectMocks NormeController normeController;

    private MockMvc mockMvc;

    // Jackson 2.x MixIn : ignore les propriétés de PageImpl qui causent des
    // problèmes de sérialisation (getPageable() → navigation infinie, getSort()).
    @JsonIgnoreProperties({"pageable", "sort"})
    abstract static class PageImplMixin {}

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setup() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(PageImpl.class, PageImplMixin.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(normeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(mapper),
                        new ResourceHttpMessageConverter())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NormeResponse minimalResponse() {
        return new NormeResponse(
                1L, "ISO-001", null, "Titre FR", null, null, null, null, null,
                false, null, null, null, false, null, false, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null
        );
    }

    // ── GET /api/normes ───────────────────────────────────────────────────────

    @Test
    void listNormes_retourne200AvecPage() throws Exception {
        when(normeService.findAll(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(minimalResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/normes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].reference").value("ISO-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listNormes_avecFiltreRecherche_appelleServiceAvecParametres() throws Exception {
        when(normeService.findAll(eq("ISO"), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/normes").param("search", "ISO"))
                .andExpect(status().isOk());

        verify(normeService).findAll(eq("ISO"), any(), any(), any(), any(), any(), any());
    }

    // ── GET /api/normes/{id} ──────────────────────────────────────────────────

    @Test
    void getNormeById_normeExistante_retourne200() throws Exception {
        when(normeService.findById(1L)).thenReturn(minimalResponse());

        mockMvc.perform(get("/api/normes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.reference").value("ISO-001"));
    }

    @Test
    void getNormeById_normeInexistante_retourne404() throws Exception {
        when(normeService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Norme not found with id=99"));

        mockMvc.perform(get("/api/normes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Norme not found with id=99"));
    }

    // ── POST /api/normes ──────────────────────────────────────────────────────

    @Test
    void createNorme_donneesValides_retourne201() throws Exception {
        when(normeService.create(any())).thenReturn(minimalResponse());

        mockMvc.perform(post("/api/normes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "ISO-001", "titreFr": "Titre FR",
                                 "includedInSubscription": false, "mandatory": false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("ISO-001"));
    }

    @Test
    void createNorme_referenceVide_retourne400() throws Exception {
        mockMvc.perform(post("/api/normes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "", "titreFr": "Titre"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createNorme_referenceAbsente_retourne400() throws Exception {
        mockMvc.perform(post("/api/normes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titreFr": "Titre sans reference"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/normes/{id} ──────────────────────────────────────────────────

    @Test
    void updateNorme_donneesValides_retourne200() throws Exception {
        when(normeService.update(eq(1L), any())).thenReturn(minimalResponse());

        mockMvc.perform(put("/api/normes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "ISO-001", "titreFr": "Titre Mis à Jour"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("ISO-001"));
    }

    @Test
    void updateNorme_normeInexistante_retourne404() throws Exception {
        when(normeService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Norme not found with id=99"));

        mockMvc.perform(put("/api/normes/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reference": "ISO-X", "titreFr": "Titre"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/normes/{id} ───────────────────────────────────────────────

    @Test
    void deleteNorme_normeExistante_retourne204() throws Exception {
        doNothing().when(normeService).delete(1L);

        mockMvc.perform(delete("/api/normes/1"))
                .andExpect(status().isNoContent());

        verify(normeService).delete(1L);
    }

    // ── GET /api/normes/{id}/pdf ──────────────────────────────────────────────

    @Test
    void downloadPdf_pdfDisponible_retourne200() throws Exception {
        byte[] contenu = "contenu pdf".getBytes();
        NormePdfDownload pdf = new NormePdfDownload(
                new ByteArrayResource(contenu), "norme.pdf", "application/pdf", (long) contenu.length);
        when(normeService.downloadPdf(1L)).thenReturn(pdf);

        mockMvc.perform(get("/api/normes/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void downloadPdf_sansAbonnementActif_retourne402() throws Exception {
        doThrow(new SubscriptionRequiredException("Abonnement requis"))
                .when(abonnementGuard).verifierEtEnregistrer(any(), eq(1L));

        mockMvc.perform(get("/api/normes/1/pdf"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value("Abonnement requis"));
    }

    // ── POST /api/normes/import/excel ─────────────────────────────────────────

    @Test
    void importExcel_fichierValide_retourne200AvecRapport() throws Exception {
        NormeImportReport rapport = new NormeImportReport(5, 5, 0, 0, 0, List.of());
        when(normeExcelImportService.importFromExcel(any(), any())).thenReturn(rapport);

        MockMultipartFile file = new MockMultipartFile(
                "file", "normes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[1]);

        mockMvc.perform(multipart("/api/normes/import/excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(5))
                .andExpect(jsonPath("$.created").value(5))
                .andExpect(jsonPath("$.errors").value(0));
    }
}
