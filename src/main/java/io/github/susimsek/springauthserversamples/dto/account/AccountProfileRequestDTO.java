package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(
        name = "AccountProfileRequest",
        description = "Profile fields the authenticated account may update.")
public record AccountProfileRequestDTO(
        @Schema(
                        description = "Optional given name.",
                        example = "Ada",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 100)
                String firstName,
        @Schema(
                        description = "Optional family name.",
                        example = "Lovelace",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 100)
                String lastName,
        @Schema(
                        description = "Optional email address.",
                        example = "ada@example.test",
                        format = "email",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Email
                @Size(max = 200)
                String email) {}
