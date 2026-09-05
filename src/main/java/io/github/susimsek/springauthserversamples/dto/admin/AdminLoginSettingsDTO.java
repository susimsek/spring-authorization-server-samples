package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminLoginSettings", description = "Application-wide public login settings.")
public record AdminLoginSettingsDTO(
        @Schema(description = "Allow visitors to create accounts.") boolean userRegistration,
        @Schema(description = "Show the forgot-password link and allow password reset requests.")
                boolean forgotPassword,
        @Schema(description = "Show and honor the remember-me option on the login form.")
                boolean rememberMe,
        @Schema(description = "Allow email addresses as login identifiers.") boolean loginWithEmail,
        @Schema(description = "Require verified email addresses for new accounts.")
                boolean verifyEmail,
        @Schema(description = "Maximum browser session duration in minutes.", minimum = "1")
                int sessionTimeoutMinutes,
        @Schema(description = "Minimum accepted password length.", minimum = "8")
                int passwordMinimumLength,
        @Schema(description = "Enable protection against repeated failed logins.")
                boolean bruteForceEnabled,
        @Schema(description = "Failed login threshold before throttling.", minimum = "1")
                int bruteForceMaxFailures,
        @Schema(description = "Allow users to enroll a TOTP authenticator.") boolean otpEnabled,
        @Schema(description = "Require TOTP after password authentication.") boolean otpRequired,
        @Schema(description = "Issuer label shown in authenticator applications.") String otpIssuer,
        @Schema(description = "TOTP algorithm (SHA1, SHA256, or SHA512).") String otpAlgorithm,
        @Schema(description = "TOTP code length (6 or 8).", minimum = "6", maximum = "8")
                int otpDigits,
        @Schema(description = "TOTP period in seconds.", minimum = "15") int otpPeriodSeconds,
        @Schema(description = "Accepted clock drift window in adjacent periods.", minimum = "0")
                int otpLookAheadWindow) {}
