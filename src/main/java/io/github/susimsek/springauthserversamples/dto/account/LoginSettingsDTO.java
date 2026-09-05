package io.github.susimsek.springauthserversamples.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginSettings", description = "Public login-page feature switches.")
public record LoginSettingsDTO(
        @Schema(description = "Whether visitors can create accounts.") boolean userRegistration,
        @Schema(description = "Whether users can request password reset emails.")
                boolean forgotPassword,
        @Schema(description = "Whether the login page offers remember-me sessions.")
                boolean rememberMe) {}
