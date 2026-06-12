package portail.web.backend.exemple.portail.web.backend.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {
    @Size(max = 100)
    private String username;

    @Size(min = 8, max = 72)
    private String password;

    @Pattern(regexp = "^(ROLE_)?(ADMIN|USER)$")
    private String role;

    public UpdateUserRequest() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
