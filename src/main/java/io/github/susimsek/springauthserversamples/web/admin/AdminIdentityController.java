package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.AdminAvatarService;
import io.github.susimsek.springauthserversamples.service.admin.AdminConsentService;
import io.github.susimsek.springauthserversamples.service.admin.AdminDashboardService;
import io.github.susimsek.springauthserversamples.service.admin.AdminRoleService;
import io.github.susimsek.springauthserversamples.service.admin.AdminServerInfoService;
import io.github.susimsek.springauthserversamples.service.admin.AdminSessionService;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.service.admin.KeyManagementService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.PasswordChangeValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@ApiController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Identity",
        description = "Keycloak-style users, roles, sessions, consents, events, and signing keys.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminIdentityController {

    private final AdminUserService adminUserService;
    private final AdminAuditEventService adminAuditEventService;
    private final AdminAvatarService adminAvatarService;
    private final AdminSessionService adminSessionService;
    private final AdminServerInfoService adminServerInfoService;
    private final AdminConsentService adminConsentService;
    private final AdminDashboardService adminDashboardService;
    private final KeyManagementService keyManagementService;
    private final AdminRoleService adminRoleService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get administration dashboard")
    AdminDashboardService.DashboardView dashboard() {
        return adminDashboardService.dashboard();
    }

    @GetMapping("/server-info")
    @Operation(summary = "Get server information")
    AdminServerInfoService.ServerInfoView serverInfo() {
        return adminServerInfoService.serverInfo();
    }

    @GetMapping("/events")
    @Operation(summary = "Search administrative events")
    Page<AdminAuditEventService.EventView> events(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String targetType,
            @RequestParam(defaultValue = "") String targetId,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @PageableDefault(
                            size = 20,
                            sort = "occurredAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminAuditEventService.events(q, action, targetType, targetId, from, to, pageable);
    }

    @GetMapping("/users/{id}/events")
    @Operation(summary = "List user events")
    Page<AdminAuditEventService.EventView> userEvents(
            @PathVariable Long id,
            @PageableDefault(
                            size = 20,
                            sort = "occurredAt",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable,
            Authentication authentication) {
        adminUserService.requireManageableUser(id, authentication.getName());
        return adminAuditEventService.userEvents(id, pageable);
    }

    @GetMapping("/users/{id}/sessions")
    @Operation(summary = "List user sessions")
    Page<AdminSessionService.SessionView> userSessions(
            @PathVariable Long id,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable,
            Authentication authentication) {
        return adminSessionService.userSessions(id, authentication.getName(), pageable);
    }

    @GetMapping("/users/{id}/consents")
    @Operation(summary = "List user consents")
    Page<AdminConsentService.ConsentView> userConsents(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "id.registeredClientId") Pageable pageable,
            Authentication authentication) {
        return adminConsentService.userConsents(id, authentication.getName(), pageable);
    }

    @GetMapping("/roles")
    @Operation(summary = "Search roles")
    Page<AdminRoleService.RoleView> roles(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return adminRoleService.roles(q, pageable);
    }

    @GetMapping("/roles/{name}")
    @Operation(summary = "Get role")
    AdminRoleService.RoleDetailView role(
            @PathVariable String name,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminRoleService.role(name, q, pageable);
    }

    @GetMapping("/roles/{name}/available-users")
    @Operation(summary = "Search users available for a role")
    Page<AdminRoleService.UserEntityView> availableRoleUsers(
            @PathVariable String name,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 10, sort = "username") Pageable pageable) {
        return adminRoleService.availableUsers(name, q, pageable);
    }

    @PostMapping("/roles/{name}/users")
    @Operation(summary = "Assign user to role")
    AdminRoleService.RoleDetailView assignRoleUser(
            @PathVariable String name,
            @Valid @RequestBody AdminRoleUserRequest request,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.assignUser(
                name, request.userId(), authentication.getName(), pageable);
    }

    @DeleteMapping("/roles/{name}/users/{userId}")
    @Operation(summary = "Remove user from role")
    AdminRoleService.RoleDetailView removeRoleUser(
            @PathVariable String name,
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "username") Pageable pageable,
            Authentication authentication) {
        return adminRoleService.removeUser(name, userId, authentication.getName(), pageable);
    }

    @PostMapping("/roles")
    @Operation(summary = "Create role")
    ResponseEntity<AdminRoleService.RoleView> createRole(
            @Valid @RequestBody AdminRoleRequest request) {
        return ResponseEntity.status(201).body(adminRoleService.createRole(request.name()));
    }

    @DeleteMapping("/roles/{name}")
    @Operation(summary = "Delete role")
    ResponseEntity<Void> deleteRole(@PathVariable String name) {
        adminRoleService.deleteRole(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Search users")
    Page<AdminUserService.UserView> users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminUserService.users(q, enabled, pageable);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user")
    AdminUserService.UserView user(@PathVariable Long id, Authentication authentication) {
        return adminUserService.user(id, authentication.getName());
    }

    @GetMapping("/users/{id}/groups")
    @Operation(summary = "List a user's groups")
    Page<AdminGroupDTO> userGroups(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Authentication authentication) {
        return adminUserService.groups(id, q, pageable, authentication.getName());
    }

    @PostMapping("/users")
    @Operation(summary = "Create user")
    ResponseEntity<AdminUserService.UserView> createUser(
            @Validated(CreateValidation.class) @RequestBody AdminUserRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201)
                .body(
                        adminUserService.createUser(
                                request.username(),
                                request.password(),
                                request.enabled() == null || request.enabled(),
                                request.roles(),
                                authentication.getName()));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user")
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
    @Operation(summary = "Upload user avatar")
    AdminAvatarService.AvatarView updateAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return adminAvatarService.updateAvatar(id, file, authentication.getName());
    }

    @DeleteMapping("/users/{id}/avatar")
    @Operation(summary = "Delete user avatar")
    ResponseEntity<Void> deleteAvatar(@PathVariable Long id, Authentication authentication) {
        adminAvatarService.deleteAvatar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/password")
    @Operation(summary = "Reset user password")
    ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Validated(PasswordChangeValidation.class) @RequestBody AdminUserRequest request,
            Authentication authentication) {
        adminUserService.changePassword(id, request.password(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user")
    ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        adminUserService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}/enabled")
    @Operation(summary = "Enable or disable user")
    ResponseEntity<Void> setUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserEnabledRequest request,
            Authentication authentication) {
        adminUserService.setUserEnabled(id, request.enabled(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    @Operation(summary = "Search browser sessions")
    Page<AdminSessionService.SessionView> sessions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String clientId,
            @RequestParam(defaultValue = "active") String status,
            @PageableDefault(
                            size = 20,
                            sort = "lastAccessTime",
                            direction = org.springframework.data.domain.Sort.Direction.DESC)
                    Pageable pageable) {
        return adminSessionService.sessions(q, clientId, status, pageable);
    }

    Page<AdminSessionService.SessionView> sessions(String q, Pageable pageable) {
        return adminSessionService.sessions(q, pageable);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get browser session")
    AdminSessionService.SessionDetailView session(
            @PathVariable String id, Authentication authentication) {
        return adminSessionService.session(id, authentication.getName());
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete browser session")
    ResponseEntity<Void> deleteSession(@PathVariable String id, Authentication authentication) {
        adminSessionService.deleteSession(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{username}/sessions")
    @Operation(summary = "Delete all sessions for a user")
    ResponseEntity<Void> deleteUserSessions(
            @PathVariable String username, Authentication authentication) {
        adminSessionService.deleteUserSessions(username, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consents")
    @Operation(summary = "Search user consents")
    Page<AdminConsentService.ConsentView> consents(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String clientId,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String scope,
            @PageableDefault(size = 20, sort = "id.principalName") Pageable pageable) {
        return adminConsentService.consents(q, clientId, username, scope, pageable);
    }

    @GetMapping("/consents/{clientId}/{username}")
    @Operation(summary = "Get user consent")
    AdminConsentService.ConsentView consent(
            @PathVariable String clientId, @PathVariable String username) {
        return adminConsentService.consent(clientId, username);
    }

    @DeleteMapping("/consents/{clientId}/{username}")
    @Operation(summary = "Revoke user consent")
    ResponseEntity<Void> revokeConsent(
            @PathVariable String clientId,
            @PathVariable String username,
            Authentication authentication) {
        adminConsentService.revokeConsent(clientId, username, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keys")
    @Operation(summary = "List signing keys")
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
    @Operation(summary = "Rotate signing key")
    KeyManagementService.KeyView rotateKey() {
        return keyManagementService.rotateKey();
    }
}
