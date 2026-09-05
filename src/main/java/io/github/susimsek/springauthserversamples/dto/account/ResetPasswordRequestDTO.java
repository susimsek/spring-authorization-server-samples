package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ResetPasswordRequest", description = "Request to reset a password with a token.")
public record ResetPasswordRequestDTO(
        @Schema(
                        description = "Single-use token received by email.",
                        example = "eyJhbGciOiJIUzI1NiJ9.reset-token",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(max = 200)
                String token,
        @Schema(
                        description = "New password; must contain at least 8 characters.",
                        example = "new-password",
                        format = "password",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank
                @Size(min = 12, max = 128)
                String newPassword) {}
