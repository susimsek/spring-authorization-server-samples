package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AdminGroupRequest", description = "A group to create or rename.")
public record AdminGroupRequestDTO(
        @NotBlank(message = "{app.api.problem.violation.required}")
                @Size(max = 100, message = "{app.api.problem.violation.max_length}")
                @Schema(
                        description = "Group name.",
                        example = "finance-operators",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
        @Schema(
                        description = "Parent group identifier for a nested group.",
                        example = "1",
                        format = "int64",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Long parentId) {}
