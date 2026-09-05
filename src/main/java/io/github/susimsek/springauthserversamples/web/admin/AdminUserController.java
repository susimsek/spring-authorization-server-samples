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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(
            summary = "Search users",
            description =
                    "Returns a paged user list with optional username and enabled-state filters.")
    @ApiResponse(responseCode = "200", description = "Paged users returned.")
    Page<AdminUserDTO> users(
            @Parameter(
                            description = "Optional username, email, or name search text.",
                            example = "user")
                    @RequestParam(defaultValue = "")
                    String q,
            @Parameter(description = "Filter by enabled state.", example = "true")
                    @RequestParam(required = false)
                    Boolean enabled,
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return adminUserService.users(q, enabled, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Returns one user by internal identifier.")
    @ApiResponse(responseCode = "200", description = "User returned.")
    AdminUserDTO user(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            Authentication authentication) {
        return adminUserService.user(id, authentication.getName());
    }

    @GetMapping("/{id}/groups")
    @Operation(
            summary = "List a user's groups",
            description = "Returns groups to which the specified user belongs.")
    @ApiResponse(responseCode = "200", description = "Paged user groups returned.")
    Page<AdminGroupDTO> userGroups(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @Parameter(
                            description = "Optional group name or path search text.",
                            example = "finance")
                    @RequestParam(defaultValue = "")
                    String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Authentication authentication) {
        return adminUserService.groups(id, q, pageable, authentication.getName());
    }

    @PostMapping
    @Operation(
            summary = "Create user",
            description =
                    "Creates a user account with credentials, enabled state, and realm roles.")
    @ApiResponse(responseCode = "201", description = "User created and returned.")
    ResponseEntity<AdminUserDTO> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "New user account fields.",
                            required = true)
                    @Validated(CreateValidation.class)
                    @RequestBody
                    AdminUserRequestDTO request,
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
    @Operation(
            summary = "Update user",
            description = "Updates editable user account fields and role assignments.")
    @ApiResponse(responseCode = "200", description = "Updated user returned.")
    AdminUserDTO updateUser(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Replacement user account fields.",
                            required = true)
                    @Validated(UpdateValidation.class)
                    @RequestBody
                    AdminUserRequestDTO request,
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
    @Operation(
            summary = "Upload user avatar",
            description = "Stores a new avatar image for the specified user.")
    @ApiResponse(responseCode = "200", description = "Avatar URL returned.")
    AdminAvatarDTO updateAvatar(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @Parameter(description = "Image file to use as the avatar.", required = true)
                    @RequestParam("file")
                    MultipartFile file,
            Authentication authentication) {
        return adminAvatarService.updateAvatar(id, file, authentication.getName());
    }

    @DeleteMapping("/{id}/avatar")
    @Operation(
            summary = "Delete user avatar",
            description = "Removes the specified user's avatar image.")
    @ApiResponse(responseCode = "204", description = "Avatar deleted.")
    ResponseEntity<Void> deleteAvatar(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            Authentication authentication) {
        adminAvatarService.deleteAvatar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @Operation(
            summary = "Reset user password",
            description = "Replaces a user's password and invalidates affected access.")
    @ApiResponse(responseCode = "204", description = "Password reset completed.")
    ResponseEntity<Void> changePassword(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Replacement password.",
                            required = true)
                    @Validated(PasswordChangeValidation.class)
                    @RequestBody
                    AdminUserRequestDTO request,
            Authentication authentication) {
        adminUserService.changePassword(id, request.password(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unlock")
    @Operation(
            summary = "Unlock user account",
            description = "Clears temporary and permanent login lock state.")
    @ApiResponse(responseCode = "204", description = "User account unlocked.")
    ResponseEntity<Void> unlockUser(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            Authentication authentication) {
        adminUserService.unlockUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete user",
            description = "Deletes the specified user and invalidates related access.")
    @ApiResponse(responseCode = "204", description = "User deleted.")
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            Authentication authentication) {
        adminUserService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enabled")
    @Operation(
            summary = "Enable or disable user",
            description = "Changes whether the specified user may authenticate.")
    @ApiResponse(responseCode = "204", description = "User enabled state updated.")
    ResponseEntity<Void> setUserEnabled(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "New enabled state.",
                            required = true)
                    @Valid
                    @RequestBody
                    AdminUserEnabledRequestDTO request,
            Authentication authentication) {
        adminUserService.setUserEnabled(id, request.enabled(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/execute-actions-email")
    @Operation(
            summary = "Send an email for required user actions",
            description = "Sends an email for exactly one supported pending user action.")
    @ApiResponse(responseCode = "204", description = "Action email queued.")
    ResponseEntity<Void> executeActionsEmail(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @Parameter(description = "Optional token lifespan in seconds.", example = "3600")
                    @RequestParam(required = false)
                    Long lifespan,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description =
                                    "JSON array containing exactly one pending action, such as"
                                            + " `VERIFY_EMAIL`.",
                            required = true,
                            content =
                                    @io.swagger.v3.oas.annotations.media.Content(
                                            mediaType = "application/json",
                                            schema =
                                                    @io.swagger.v3.oas.annotations.media.Schema(
                                                            type = "array",
                                                            example = "[\"VERIFY_EMAIL\"]")))
                    @RequestBody
                    Set<UserAction> actions,
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
    @Operation(
            summary = "Send a verification email",
            description = "Sends an email containing a verification action link.")
    @ApiResponse(responseCode = "204", description = "Verification email queued.")
    ResponseEntity<Void> sendVerifyEmail(
            @Parameter(description = "Internal user identifier.", example = "2", required = true)
                    @PathVariable
                    Long id,
            @Parameter(description = "Optional token lifespan in seconds.", example = "3600")
                    @RequestParam(required = false)
                    Long lifespan,
            Locale locale,
            Authentication authentication) {
        adminUserService.executeActionsEmail(
                id, UserAction.VERIFY_EMAIL, lifespan, locale, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
