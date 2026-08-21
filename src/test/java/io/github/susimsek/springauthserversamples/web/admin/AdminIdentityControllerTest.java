package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.service.admin.AdminAvatarService;
import io.github.susimsek.springauthserversamples.service.admin.AdminConsentService;
import io.github.susimsek.springauthserversamples.service.admin.AdminDashboardService;
import io.github.susimsek.springauthserversamples.service.admin.AdminRoleService;
import io.github.susimsek.springauthserversamples.service.admin.AdminSessionService;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.service.admin.KeyManagementService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AdminIdentityControllerTest {

    private final AdminUserService adminUserService = mock(AdminUserService.class);
    private final AdminAvatarService adminAvatarService = mock(AdminAvatarService.class);
    private final AdminSessionService adminSessionService = mock(AdminSessionService.class);
    private final AdminConsentService adminConsentService = mock(AdminConsentService.class);
    private final AdminDashboardService adminDashboardService = mock(AdminDashboardService.class);
    private final KeyManagementService keyManagementService = mock(KeyManagementService.class);
    private final AdminRoleService adminRoleService = mock(AdminRoleService.class);
    private final AdminIdentityController controller =
            new AdminIdentityController(
                    adminUserService,
                    adminAvatarService,
                    adminSessionService,
                    adminConsentService,
                    adminDashboardService,
                    keyManagementService,
                    adminRoleService);

    @Test
    void delegatesDashboardAndRoleEndpoints() {
        var dashboard = new AdminDashboardService.DashboardView(4, 5, 6, 7);
        var roles = List.of(new AdminRoleService.RoleView("ROLE_ADMIN"));
        when(adminDashboardService.dashboard()).thenReturn(dashboard);
        when(adminRoleService.roles()).thenReturn(roles);
        when(adminRoleService.createRole("ROLE_AUDITOR"))
                .thenReturn(new AdminRoleService.RoleView("ROLE_AUDITOR"));

        assertThat(controller.dashboard()).isSameAs(dashboard);
        assertThat(controller.roles()).containsExactlyElementsOf(roles);
        assertThat(controller.createRole(new AdminRoleRequest("ROLE_AUDITOR")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(controller.createRole(new AdminRoleRequest("ROLE_AUDITOR")).getBody().name())
                .isEqualTo("ROLE_AUDITOR");
        assertThat(controller.deleteRole("ROLE_AUDITOR").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminRoleService).deleteRole("ROLE_AUDITOR");
    }

    @Test
    void delegatesUserEndpoints() {
        var authentication = authentication();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(userView()));
        AdminUserRequest request =
                new AdminUserRequest("alice", "password123", true, Set.of("ROLE_USER"));
        when(adminUserService.users("ali", true, pageable)).thenReturn(page);
        when(adminUserService.user(1L, "admin")).thenReturn(userView());
        when(adminUserService.createUser("alice", "password123", true, Set.of("ROLE_USER")))
                .thenReturn(userView());
        when(adminUserService.updateUser(1L, "alice", true, Set.of("ROLE_USER"), "admin"))
                .thenReturn(userView());

        assertThat(controller.users("ali", true, pageable)).isSameAs(page);
        assertThat(controller.user(1L, authentication)).isEqualTo(userView());
        assertThat(controller.createUser(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateUser(1L, request, authentication)).isEqualTo(userView());
        assertThat(controller.changePassword(1L, request, authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteUser(1L, authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(
                        controller
                                .setUserEnabled(
                                        1L, new AdminUserEnabledRequest(false), authentication)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminUserService).changePassword(1L, "password123", "admin");
        verify(adminUserService).deleteUser(1L, "admin");
        verify(adminUserService).setUserEnabled(1L, false, "admin");
    }

    @Test
    void delegatesAvatarEndpoints() {
        var authentication = authentication();
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1, 2, 3});
        var view = new AdminAvatarService.AvatarView("/avatars/test?v=1");
        when(adminAvatarService.updateAvatar(1L, file, "admin")).thenReturn(view);

        assertThat(controller.updateAvatar(1L, file, authentication)).isSameAs(view);
        assertThat(controller.deleteAvatar(1L, authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminAvatarService).deleteAvatar(1L, "admin");
    }

    @Test
    void delegatesSessionEndpoints() {
        var authentication = authentication();
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(sessionView()));
        when(adminSessionService.sessions("adm", pageable)).thenReturn(page);

        assertThat(controller.sessions("adm", pageable)).isSameAs(page);
        assertThat(controller.deleteSession("sess-1", authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteUserSessions("alice", authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        verify(adminSessionService).deleteSession("sess-1", "admin");
        verify(adminSessionService).deleteUserSessions("alice", "admin");
    }

    @Test
    void delegatesConsentAndKeyEndpoints() {
        var authentication = authentication();
        var consentPage = new PageImpl<>(List.of(consentView()));
        var keyPage = new PageImpl<>(List.of(keyView()));
        var pageable = PageRequest.of(0, 20);
        when(adminConsentService.consents("ali", pageable)).thenReturn(consentPage);
        when(keyManagementService.keys("kid", true, pageable)).thenReturn(keyPage);
        when(keyManagementService.rotateKey()).thenReturn(keyView());

        assertThat(controller.consents("ali", pageable)).isSameAs(consentPage);
        assertThat(controller.revokeConsent("client-1", "alice", authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.keys("kid", true, pageable)).isSameAs(keyPage);
        assertThat(controller.rotateKey()).isEqualTo(keyView());
        verify(adminConsentService).revokeConsent("client-1", "alice", "admin");
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        return UsernamePasswordAuthenticationToken.authenticated("admin", "ignored", List.of());
    }

    private static AdminUserService.UserView userView() {
        return new AdminUserService.UserView(
                1L, "alice", true, "/avatars/a?v=1", Set.of("ROLE_USER"));
    }

    private static AdminSessionService.SessionView sessionView() {
        Instant now = Instant.parse("2026-08-21T00:00:00Z");
        return new AdminSessionService.SessionView("sess-1", "alice", now, now, now, 2);
    }

    private static AdminConsentService.ConsentView consentView() {
        return new AdminConsentService.ConsentView(
                "client-1", "Client One", "alice", Set.of("openid"));
    }

    private static KeyManagementService.KeyView keyView() {
        return new KeyManagementService.KeyView(
                "key-1",
                "kid-1",
                "RSA",
                "RS256",
                "sig",
                true,
                Instant.parse("2026-08-21T00:00:00Z"));
    }
}
