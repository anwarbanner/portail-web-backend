package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import portail.web.backend.exemple.portail.web.backend.exception.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires du GlobalExceptionHandler via un contrôleur fictif.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void notFound() { throw new ResourceNotFoundException("Resource not found"); }

        @GetMapping("/test/bad-request")
        public void badRequest() { throw new BadRequestException("Bad input"); }

        @GetMapping("/test/subscription")
        public void subscription() { throw new SubscriptionRequiredException("Abonnement requis"); }

        @GetMapping("/test/consultation-limit")
        public void consultationLimit() { throw new ConsultationLimitExceededException("Limite atteinte"); }

        @GetMapping("/test/access-denied")
        public void accessDenied() { throw new AccessDeniedException("Forbidden"); }

        @GetMapping("/test/generic")
        public void generic() { throw new RuntimeException("Unexpected error"); }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void resourceNotFoundException_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void badRequestException_returns400() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Bad input"));
    }

    @Test
    void subscriptionRequiredException_returns402() throws Exception {
        mockMvc.perform(get("/test/subscription"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("Abonnement requis"));
    }

    @Test
    void consultationLimitExceededException_returns403() throws Exception {
        mockMvc.perform(get("/test/consultation-limit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Limite atteinte"));
    }

    @Test
    void accessDeniedException_returns403WithAccessDeniedMessage() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void genericException_returns500() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
