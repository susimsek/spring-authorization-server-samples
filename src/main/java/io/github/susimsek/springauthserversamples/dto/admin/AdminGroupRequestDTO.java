package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminGroupRequest", description = "A group to create or rename.")
public record AdminGroupRequestDTO(
        @NotBlank(message = "{app.api.problem.violation.required}")
                @Size(max = 100, message = "{app.api.problem.violation.max_length}")
                @Schema(example = "finance-operators")
                String name) {}
