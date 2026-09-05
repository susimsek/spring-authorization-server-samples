package io.github.susimsek.springauthserversamples.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiViolation", description = "A field that failed request validation.")
public record ApiViolationDTO(
        @Schema(description = "JSON field name that failed validation.", example = "clientId")
                String field) {}
