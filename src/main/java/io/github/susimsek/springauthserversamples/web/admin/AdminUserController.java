package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.config.openapi.OpenApiConfig;
import io.github.susimsek.springauthserversamples.domain.UserAction;
import io.github.susimsek.springauthserversamples.dto.admin.AdminAvatarDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminUserDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminUserEnabledRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminUserRequestDTO;
import io.github.susimsek.springauthserversamples.service.admin.AdminAvatarService;
import io.github.susimsek.springauthserversamples.service.admin.AdminUserService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.github.susimsek.springauthserversamples.web.admin.validation.CreateValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.PasswordChangeValidation;
import io.github.susimsek.springauthserversamples.web.admin.validation.UpdateValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Set;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "User and credential administration.")
@SecurityRequirement(name = OpenApiConfig.ADMIN_BEARER)
class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminAvatarService adminAvatarService;

    @GetMapping
    @Operation(summary = "Search users")
    Page<AdminUserDTO> users(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminUserService.users(q, enabled, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user")
    AdminUserDTO user(@PathVariable Long id, Authentication authentication) {
        return adminUserService.user(id, authentication.getName());
    }

    @GetMapping("/{id}/groups")
    @Operation(summary = "List a user's groups")
    Page<AdminGroupDTO> userGroups(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Authentication authentication) {
        return adminUserService.groups(id, q, pageable, authentication.getName());
    }

    @PostMapping
    @Operation(summary = "Create user")
    ResponseEntity<AdminUserDTO> createUser(
            @Validated(CreateValidation.class) @RequestBody AdminUserRequestDTO request,
            Authentication authentication) {
        return ResponseEntity.status(201)
                .body(
                        adminUserService.createUser(
                                request.username(),
                                request.email(),
                                Boolean.TRUE.equals(request.emailVerified()),
                                request.password(),
                                request.enabled() == null || request.enabled(),
                                request.roles(),
                                authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    AdminUserDTO updateUser(
            @PathVariable Long id,
            @Validated(UpdateValidation.class) @RequestBody AdminUserRequestDTO request,
            Authentication authentication) {
        return adminUserService.updateUser(
                id,
                request.username(),
                request.email(),
                Boolean.TRUE.equals(request.emailVerified()),
                request.enabled() == null || request.enabled(),
                request.roles(),
                authentication.getName());
    }

    @PutMapping(path = "/{id}/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Upload user avatar")
    AdminAvatarDTO updateAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return adminAvatarService.updateAvatar(id, file, authentication.getName());
    }

    @DeleteMapping("/{id}/avatar")
    @Operation(summary = "Delete user avatar")
    ResponseEntity<Void> deleteAvatar(@PathVariable Long id, Authentication authentication) {
        adminAvatarService.deleteAvatar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Reset user password")
    ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Validated(PasswordChangeValidation.class) @RequestBody AdminUserRequestDTO request,
            Authentication authentication) {
        adminUserService.changePassword(id, request.password(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        adminUserService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable user")
    ResponseEntity<Void> setUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserEnabledRequestDTO request,
            Authentication authentication) {
        adminUserService.setUserEnabled(id, request.enabled(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/execute-actions-email")
    @Operation(summary = "Send an email for required user actions")
    ResponseEntity<Void> executeActionsEmail(
            @PathVariable Long id,
            @RequestParam(required = false) Long lifespan,
            @RequestBody Set<UserAction> actions,
            Locale locale,
            Authentication authentication) {
        if (actions == null || actions.size() != 1) {
            throw io.github.susimsek.springauthserversamples.service.error.ApiException.badRequest(
                    io.github.susimsek.springauthserversamples.service.error.ApiErrorCode
                            .ACTION_UNSUPPORTED,
                    "Exactly one supported action is required");
        }
        adminUserService.executeActionsEmail(
                id, actions.iterator().next(), lifespan, locale, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/send-verify-email")
    @Operation(summary = "Send a verification email")
    ResponseEntity<Void> sendVerifyEmail(
            @PathVariable Long id,
            @RequestParam(required = false) Long lifespan,
            Locale locale,
            Authentication authentication) {
        adminUserService.executeActionsEmail(
                id, UserAction.VERIFY_EMAIL, lifespan, locale, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
