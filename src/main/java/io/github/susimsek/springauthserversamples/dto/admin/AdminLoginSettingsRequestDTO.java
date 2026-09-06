package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
                int bruteForceMaxFailures,
        @Schema(
                        description =
                                "Maximum failed MFA challenges before permanent account lockout."
                                        + " Set to zero to disable.",
                        minimum = "0")
                @Min(0)
                int bruteForceMaxSecondaryFailures,
        @Schema(description = "Allow users to enroll a TOTP authenticator.") boolean otpEnabled,
        @Schema(description = "Require TOTP after password authentication.") boolean otpRequired,
        @Schema(description = "Issuer label shown in authenticator applications.")
                @NotBlank
                @jakarta.validation.constraints.Size(max = 100)
                String otpIssuer,
        @Schema(description = "TOTP algorithm (SHA1, SHA256, or SHA512).")
                @NotBlank
                @Pattern(regexp = "(?i)SHA1|SHA256|SHA512")
                String otpAlgorithm,
        @Schema(description = "TOTP code length (6 or 8).", minimum = "6", maximum = "8")
                @Min(6)
                @Max(8)
                int otpDigits,
        @Schema(description = "TOTP period in seconds.", minimum = "15") @Min(15)
                int otpPeriodSeconds,
        @Schema(description = "Accepted clock drift window in adjacent periods.", minimum = "0")
                @Min(0)
                int otpLookAheadWindow,
        @Schema(description = "Allow the same TOTP code to be used more than once.")
                boolean otpCodeReusable,
        @Schema(description = "Request recovery codes after OTP setup.")
                boolean otpAddRecoveryCodes,
        @Schema(
                        description = "Show a warning when unused recovery codes reach this count.",
                        minimum = "0")
                @Min(0)
                int recoveryCodeWarningThreshold) {}
