package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Password change request for the authenticated account.")
public record AccountPasswordRequestDTO(
        @Schema(example = "current-password", format = "password") @NotBlank @Size(max = 200)
                String currentPassword,
        @Schema(example = "new-password", format = "password") @NotBlank @Size(min = 8, max = 200)
                String newPassword) {}
