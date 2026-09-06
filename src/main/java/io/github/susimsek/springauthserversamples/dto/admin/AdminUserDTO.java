package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Set;

@Schema(name = "AdminUser", description = "User account and effective authorities.")
public record AdminUserDTO(
        @Schema(
                        description = "Internal user identifier.",
                        example = "2",
                        format = "int64",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Long id,
        @Schema(
                        description = "Unique login name.",
                        example = "user",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String username,
        @Schema(
                        description = "Email address.",
                        example = "user@example.test",
                        format = "email",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String email,
        @Schema(
                        description = "Whether the email address is verified.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean emailVerified,
        @Schema(
                        description = "Whether the account is enabled.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean enabled,
        @Schema(description = "Whether the account is currently locked.", example = "false")
                boolean locked,
        @Schema(
                        description = "Automatic unlock time, if temporarily locked.",
                        example = "2026-09-04T09:00:00Z",
                        format = "date-time",
                        nullable = true)
                Instant lockedUntil,
        @Schema(description = "Number of failed login attempts.", example = "2")
                int failedLoginCount,
        @Schema(description = "Whether the user must change the password.", example = "false")
                boolean mustChangePassword,
        @Schema(
                        description = "Whether the current password was assigned temporarily.",
                        example = "false")
                boolean temporaryPassword,
        @Schema(description = "Whether a TOTP authenticator is configured.", example = "true")
                boolean totpEnabled,
        @Schema(
                        description = "Public avatar URL, if available.",
                        example = "/avatars/user-avatar-123?v=1725438600000",
                        format = "uri-reference",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String avatarUrl,
        @Schema(
                        description = "Effective authorities.",
                        example = "[\"ROLE_USER\"]",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Set<String> authorities,
        @Schema(
                        description = "Account creation time.",
                        example = "2026-09-04T08:30:00Z",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant createdAt,
        @Schema(
                        description = "Last account update time.",
                        example = "2026-09-04T08:30:00Z",
                        format = "date-time",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Instant updatedAt) {

    public AdminUserDTO(
            Long id,
            String username,
            boolean enabled,
            String avatarUrl,
            Set<String> authorities,
            Instant createdAt,
            Instant updatedAt) {
        this(
                id,
                username,
                null,
                false,
                enabled,
                false,
                null,
                0,
                false,
                false,
                false,
                avatarUrl,
                authorities,
                createdAt,
                updatedAt);
    }
}
