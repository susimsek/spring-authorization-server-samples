package io.github.susimsek.springauthserversamples.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** OpenAPI representation of the RFC 9457 response returned by application REST APIs. */
@Schema(name = "ApiProblem", description = "RFC 9457 API error response.")
public record ApiProblemDTO(
        @Schema(example = "urn:problem:user_invalid_username") String type,
        @Schema(example = "API request failed") String title,
        @Schema(example = "Username is already registered.") String detail,
        @Schema(example = "409") int status,
        @Schema(example = "/api/admin/users") String instance,
        @Schema(example = "user_duplicate_username") String errorCode,
        List<ApiViolationDTO> violations) {}
