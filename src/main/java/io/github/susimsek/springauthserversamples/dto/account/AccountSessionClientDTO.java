package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Client associated with an authenticated browser session.")
public record AccountSessionClientDTO(
        @Schema(example = "account-console") String clientId,
        @Schema(example = "Account Console") String clientName) {}
