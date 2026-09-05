package io.github.susimsek.springauthserversamples.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "AdminEmailSettingsRequest",
        description = "Updated SMTP and outgoing email settings.")
public record AdminEmailSettingsRequestDTO(
        @Schema(description = "Enable outgoing email delivery.") boolean enabled,
        @Schema(description = "Address used in the From header.") @NotBlank String fromAddress,
        @Schema(description = "Public application URL used in email links.") @NotBlank
                String baseUrl,
        @Schema(description = "SMTP server host name.") @NotBlank String host,
        @Schema(description = "SMTP server port.", minimum = "1", maximum = "65535")
                @Min(1)
                @Max(65535)
                int port,
        @Schema(description = "SMTP username, if authentication is enabled.") String username,
        @Schema(
                        description = "SMTP password. Leave blank to retain the stored password.",
                        format = "password")
                String password,
        @Schema(description = "Enable SMTP authentication.") boolean smtpAuth,
        @Schema(description = "Enable STARTTLS.") boolean starttls,
        @Schema(description = "Enable SSL/TLS socket transport.") boolean ssl) {}
