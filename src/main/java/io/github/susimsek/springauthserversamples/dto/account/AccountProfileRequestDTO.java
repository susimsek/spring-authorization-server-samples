package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Profile fields the authenticated account may update.")
public record AccountProfileRequestDTO(
        @Schema(example = "Ada") @Size(max = 100) String firstName,
        @Schema(example = "Lovelace") @Size(max = 100) String lastName,
        @Schema(example = "ada@example.test") @Email @Size(max = 200) String email) {}
