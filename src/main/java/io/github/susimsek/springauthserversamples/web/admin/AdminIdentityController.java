package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.service.admin.AdminAvatarService;
import io.github.susimsek.springauthserversamples.service.admin.AdminConsentService;
import io.github.susimsek.springauthserversamples.service.admin.AdminDashboardService;
import io.github.susimsek.springauthserversamples.service.admin.AdminRoleService;
import io.github.susimsek.springauthserversamples.service.admin.AdminSessionService;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.service.admin.KeyManagementService;
import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.PasswordChangeValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AdminApi
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminIdentityController {

    private final AdminUserService adminUserService;
    private final AdminAvatarService adminAvatarService;
    private final AdminSessionService adminSessionService;
    private final AdminConsentService adminConsentService;
    private final AdminDashboardService adminDashboardService;
    private final KeyManagementService keyManagementService;
    private final AdminRoleService adminRoleService;

    @GetMapping("/dashboard")
    AdminDashboardService.DashboardView dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/roles")
    List<AdminRoleService.RoleView> roles() {
        return adminRoleService.roles();
    }

    @PostMapping("/roles")
    ResponseEntity<AdminRoleService.RoleView> createRole(
            @Valid @RequestBody AdminRoleRequest request) {
        return ResponseEntity.status(201).body(adminRoleService.createRole(request.name()));
    }

    @DeleteMapping("/roles/{name}")
    ResponseEntity<Void> deleteRole(@PathVariable String name) {
        adminRoleService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    Page<AdminUserService.UserView> users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminUserService.users(q, enabled, pageable);
    }

    @GetMapping("/users/{id}")
    AdminUserService.UserView user(@PathVariable Long id, Authentication authentication) {
        return adminUserService.user(id, authentication.getName());
    }

    @PostMapping("/users")
    ResponseEntity<AdminUserService.UserView> createUser(
            @Validated(CreateValidation.class) @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(201)
                .body(
                        adminUserService.createUser(
                                request.username(),
                                request.password(),
                                request.enabled() == null || request.enabled(),
                                request.roles()));
    }

    @PutMapping("/users/{id}")
    AdminUserService.UserView updateUser(
            @PathVariable Long id,
            @Validated(UpdateValidation.class) @RequestBody AdminUserRequest request,
            Authentication authentication) {
        return adminUserService.updateUser(
                id,
                request.username(),
                request.enabled() == null || request.enabled(),
                request.roles(),
                authentication.getName());
    }

    @PutMapping(path = "/users/{id}/avatar", consumes = "multipart/form-data")
    AdminAvatarService.AvatarView updateAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return adminAvatarService.updateAvatar(id, file, authentication.getName());
    }

    @DeleteMapping("/users/{id}/avatar")
    ResponseEntity<Void> deleteAvatar(@PathVariable Long id, Authentication authentication) {
        adminAvatarService.deleteAvatar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/password")
    ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Validated(PasswordChangeValidation.class) @RequestBody AdminUserRequest request,
            Authentication authentication) {
        adminUserService.changePassword(id, request.password(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        adminUserService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/enabled")
    ResponseEntity<Void> setUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserEnabledRequest request,
            Authentication authentication) {
        adminUserService.setUserEnabled(id, request.enabled(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    Page<AdminSessionService.SessionView> sessions(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminSessionService.sessions(q, pageable);
    }

    @DeleteMapping("/sessions/{id}")
    ResponseEntity<Void> deleteSession(@PathVariable String id, Authentication authentication) {
        adminSessionService.deleteSession(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{username}/sessions")
    ResponseEntity<Void> deleteUserSessions(
            @PathVariable String username, Authentication authentication) {
        adminSessionService.deleteUserSessions(username, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consents")
    Page<AdminConsentService.ConsentView> consents(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "id.principalName") Pageable pageable) {
        return adminConsentService.consents(q, pageable);
    }

    @DeleteMapping("/consents/{clientId}/{username}")
    ResponseEntity<Void> revokeConsent(
            @PathVariable String clientId,
            @PathVariable String username,
            Authentication authentication) {
        adminConsentService.revokeConsent(clientId, username, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keys")
    Page<KeyManagementService.KeyView> keys(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(
                            size = 20,
                            sort = "createdAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return keyManagementService.keys(q, active, pageable);
    }

    @PostMapping("/keys/rotate")
    KeyManagementService.KeyView rotateKey() {
        return keyManagementService.rotateKey();
    }
}
