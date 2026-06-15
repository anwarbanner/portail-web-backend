package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;
import portail.web.backend.exemple.portail.web.backend.user.UserService;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserRequest;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserResponse;
import portail.web.backend.exemple.portail.web.backend.user.dto.UserUpdateRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("alice", "encoded_pw", "ROLE_USER");
        user.setId(1L);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_noFilter_returnsAll() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAll(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("alice");
    }

    @Test
    void findAll_usernameOnly_callsUsernameFilter() {
        when(userRepository.findByUsernameContainingIgnoreCase(eq("ali"), any()))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAll("ali", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByUsernameContainingIgnoreCase(eq("ali"), any());
    }

    @Test
    void findAll_roleOnly_callsRoleFilter() {
        when(userRepository.findByRole(eq("ROLE_USER"), any()))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAll(null, "ROLE_USER", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole(eq("ROLE_USER"), any());
    }

    @Test
    void findAll_bothFilters_callsCombinedFilter() {
        when(userRepository.findByUsernameContainingIgnoreCaseAndRole(eq("ali"), eq("ROLE_USER"), any()))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAll("ali", "ROLE_USER", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByUsernameContainingIgnoreCaseAndRole(eq("ali"), eq("ROLE_USER"), any());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("ROLE_USER");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_success_returnsResponse() {
        UserRequest request = new UserRequest("bob", "password123", "ROLE_USER");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        User saved = new User("bob", "encoded", "ROLE_USER");
        saved.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.create(request);

        assertThat(response.username()).isEqualTo("bob");
        assertThat(response.role()).isEqualTo("ROLE_USER");
    }

    @Test
    void create_usernameTaken_throwsBadRequest() {
        UserRequest request = new UserRequest("alice", "password123", "ROLE_USER");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void create_invalidRole_throwsBadRequest() {
        UserRequest request = new UserRequest("carol", "password123", "ROLE_UNKNOWN");
        when(userRepository.existsByUsername("carol")).thenReturn(false);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_success_returnsUpdatedResponse() {
        UserUpdateRequest request = new UserUpdateRequest("alice2", null, "ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice2")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.update(1L, request);

        assertThat(response).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        UserUpdateRequest request = new UserUpdateRequest("x", null, "ROLE_USER");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_usernameTakenByOther_throwsBadRequest() {
        UserUpdateRequest request = new UserUpdateRequest("bob", null, "ROLE_USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bob");
    }

    @Test
    void update_invalidRole_throwsBadRequest() {
        UserUpdateRequest request = new UserUpdateRequest("alice", null, "ROLE_INVALID");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_found_callsDelete() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
