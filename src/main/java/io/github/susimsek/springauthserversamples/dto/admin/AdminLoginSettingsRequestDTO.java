package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(
        name = "AdminLoginSettingsRequest",
        description = "Updated application-wide login settings.")
public record AdminLoginSettingsRequestDTO(
        @Schema(description = "Allow visitors to create accounts.") boolean userRegistration,
        @Schema(description = "Show the forgot-password link and allow password reset requests.")
                boolean forgotPassword,
        @Schema(description = "Show and honor the remember-me option on the login form.")
                boolean rememberMe,
        @Schema(description = "Allow email addresses as login identifiers.") boolean loginWithEmail,
        @Schema(description = "Require verified email addresses for new accounts.")
                boolean verifyEmail,
        @Schema(description = "Maximum browser session duration in minutes.", minimum = "1") @Min(1)
                int sessionTimeoutMinutes,
        @Schema(description = "Minimum accepted password length.", minimum = "8") @Min(8) @Max(128)
                int passwordMinimumLength,
        @Schema(description = "Enable protection against repeated failed logins.")
                boolean bruteForceEnabled,
        @Schema(description = "Failed login threshold before throttling.", minimum = "1") @Min(1)
                int bruteForceMaxFailures) {}
