package portail.web.backend.exemple.portail.web.backend.junit_test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import portail.web.backend.exemple.portail.web.backend.auth.dto.UpdateUserRequest;
import portail.web.backend.exemple.portail.web.backend.auth.dto.UserResponse;
import portail.web.backend.exemple.portail.web.backend.exception.BadRequestException;
import portail.web.backend.exemple.portail.web.backend.exception.ResourceNotFoundException;
import portail.web.backend.exemple.portail.web.backend.service.UserAdminService;
import portail.web.backend.exemple.portail.web.backend.user.User;
import portail.web.backend.exemple.portail.web.backend.user.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserAdminService userAdminService;

    private User user;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        user = new User("alice", "encoded_pw", "ROLE_USER");
        user.setId(1L);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_noFilter_returnsAll() {
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userAdminService.findAll(pageable, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        verify(userRepository).findAll(pageable);
    }

    @Test
    void findAll_usernameOnly_callsUsernameFilter() {
        when(userRepository.findByUsernameContainingIgnoreCase(eq("ali"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userAdminService.findAll(pageable, "ali", null);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByUsernameContainingIgnoreCase("ali", pageable);
    }

    @Test
    void findAll_roleOnly_callsRoleFilter() {
        when(userRepository.findByRole(eq("ROLE_USER"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userAdminService.findAll(pageable, null, "ROLE_USER");

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole("ROLE_USER", pageable);
    }

    @Test
    void findAll_roleWithoutPrefix_normalizesRole() {
        when(userRepository.findByRole(eq("ROLE_ADMIN"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        userAdminService.findAll(pageable, null, "ADMIN");

        verify(userRepository).findByRole("ROLE_ADMIN", pageable);
    }

    @Test
    void findAll_bothFilters_callsCombinedFilter() {
        when(userRepository.findByUsernameContainingIgnoreCaseAndRole(eq("ali"), eq("ROLE_USER"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userAdminService.findAll(pageable, "ali", "ROLE_USER");

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByUsernameContainingIgnoreCaseAndRole("ali", "ROLE_USER", pageable);
    }

    @Test
    void findAll_blankUsername_treatedAsNoFilter() {
        when(userRepository.findByRole(eq("ROLE_USER"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));

        userAdminService.findAll(pageable, "   ", "ROLE_USER");

        verify(userRepository).findByRole("ROLE_USER", pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_found_returnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userAdminService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_username_success() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("alice2");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("alice2")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        UserResponse result = userAdminService.update(1L, request);

        assertThat(result).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void update_sameUsername_noConflictCheck() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("alice");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userAdminService.update(1L, request);

        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void update_usernameTakenByOther_throwsBadRequest() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("bob");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void update_password_encodesAndSaves() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new_encoded");
        when(userRepository.save(any())).thenReturn(user);

        userAdminService.update(1L, request);

        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(user);
    }

    @Test
    void update_role_withoutPrefix_normalizesAndSaves() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setRole("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userAdminService.update(1L, request);

        assertThat(user.getRole()).isEqualTo("ROLE_ADMIN");
        verify(userRepository).save(user);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.update(99L, new UpdateUserRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_found_callsDeleteById() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userAdminService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userAdminService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
