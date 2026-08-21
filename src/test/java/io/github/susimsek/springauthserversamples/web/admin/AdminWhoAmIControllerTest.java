package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class AdminWhoAmIControllerTest {

    private final AdminWhoAmIController controller = new AdminWhoAmIController();

    @Test
    void returnsCurrentAdminIdentityAndAccess() {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin", "ignored", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        var response = controller.whoAmI(authentication);

        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.authorities()).containsExactly("ROLE_ADMIN");
        assertThat(response.access())
                .containsEntry("viewClients", true)
                .containsEntry("manageClients", true)
                .containsEntry("viewUsers", true)
                .containsEntry("manageUsers", true)
                .containsEntry("viewSessions", true)
                .containsEntry("manageSessions", true)
                .containsEntry("viewConsents", true)
                .containsEntry("manageConsents", true)
                .containsEntry("viewKeys", true)
                .containsEntry("manageKeys", true);
    }

    @Test
    void derivesGranularAccessFromAuthorities() {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "viewer",
                        "ignored",
                        List.of(new SimpleGrantedAuthority("ROLE_CLIENT_VIEWER")));

        var response = controller.whoAmI(authentication);

        assertThat(response.access())
                .containsEntry("viewClients", true)
                .containsEntry("manageClients", false)
                .containsEntry("viewUsers", false)
                .containsEntry("manageUsers", false)
                .containsEntry("viewSessions", false)
                .containsEntry("manageSessions", false)
                .containsEntry("viewConsents", false)
                .containsEntry("manageConsents", false)
                .containsEntry("viewKeys", false)
                .containsEntry("manageKeys", false);
    }
}
