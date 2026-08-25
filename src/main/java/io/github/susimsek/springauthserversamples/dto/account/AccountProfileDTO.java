package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Authenticated account profile.")
public record AccountProfileDTO(
        @Schema(example = "user") String username,
        @Schema(example = "Ada") String firstName,
        @Schema(example = "Lovelace") String lastName,
        @Schema(example = "ada@example.test") String email,
        Instant createdAt,
        Instant updatedAt) {}
