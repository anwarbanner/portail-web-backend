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
import portail.web.backend.exemple.portail.web.backend.controller.AdminUserController;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.GlobalExceptionHandler;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.UserService;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserRequest;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserResponse;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserUpdateRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerMvcTest {

    @Mock UserService userService;
    @InjectMocks AdminUserController adminUserController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String BASE = "/api/admin/users";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private UserResponse userResponse(Long id, String username, String role) {
        return new UserResponse(id, username, role);
    }

    // ── GET /api/admin/users ──────────────────────────────────────────────────

    @Test
    void listUsers_returns200WithPage() throws Exception {
        when(userService.findAll(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(
                        List.of(userResponse(1L, "alice", "ROLE_USER")),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listUsers_withFilters_callsServiceWithFilters() throws Exception {
        when(userService.findAll(eq("ali"), eq("ROLE_USER"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(BASE).param("username", "ali").param("role", "ROLE_USER"))
                .andExpect(status().isOk());

        verify(userService).findAll(eq("ali"), eq("ROLE_USER"), any());
    }

    // ── GET /api/admin/users/{id} ─────────────────────────────────────────────

    @Test
    void getUser_found_returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(userResponse(1L, "alice", "ROLE_USER"));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        when(userService.findById(99L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    // ── POST /api/admin/users ─────────────────────────────────────────────────

    @Test
    void createUser_validRequest_returns201() throws Exception {
        UserRequest request = new UserRequest("bob", "password123", "ROLE_USER");
        when(userService.create(any())).thenReturn(userResponse(2L, "bob", "ROLE_USER"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void createUser_blankUsername_returns400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "password": "password123", "role": "ROLE_USER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_usernameTaken_returns400() throws Exception {
        UserRequest request = new UserRequest("alice", "password123", "ROLE_USER");
        when(userService.create(any()))
                .thenThrow(new BadRequestException("Username 'alice' is already taken"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username 'alice' is already taken"));
    }

    // ── PUT /api/admin/users/{id} ─────────────────────────────────────────────

    @Test
    void updateUser_validRequest_returns200() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest("alice2", null, "ROLE_ADMIN");
        when(userService.update(eq(1L), any())).thenReturn(userResponse(1L, "alice2", "ROLE_ADMIN"));

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice2"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void updateUser_notFound_returns404() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest("nobody", null, "ROLE_USER");
        when(userService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(put(BASE + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/admin/users/{id} ──────────────────────────────────────────

    @Test
    void deleteUser_found_returns204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
                .when(userService).delete(99L);

        mockMvc.perform(delete(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }
}
