package io.github.susimsek.springauthserversamples.dto.admin;

import io.github.susimsek.springauthserversamples.web.admin.validation.AbsoluteUri;
import io.github.susimsek.springauthserversamples.web.admin.validation.PositiveDuration;
import io.github.susimsek.springauthserversamples.web.admin.validation.ValidAdminClientConfiguration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Set;

@ValidAdminClientConfiguration
public record AdminClientRequestDTO(
        @NotBlank(message = "{app.api.problem.violation.required}") @Size(max = 100)
                String clientId,
        @NotBlank(message = "{app.api.problem.violation.required}") @Size(max = 200)
                String clientName,
        @NotEmpty(message = "{app.api.problem.violation.selection}")
                Set<@NotBlank(message = "{app.api.problem.violation.selection}") String>
                        clientAuthenticationMethods,
        @NotEmpty(message = "{app.api.problem.violation.selection}")
                Set<@NotBlank(message = "{app.api.problem.violation.selection}") String>
                        authorizationGrantTypes,
        Set<@AbsoluteUri String> redirectUris,
        Set<@AbsoluteUri String> postLogoutRedirectUris,
        @NotEmpty(message = "{app.api.problem.violation.scope}")
                Set<@NotBlank(message = "{app.api.problem.violation.scope}") String> scopes,
        boolean requireAuthorizationConsent,
        boolean requireProofKey,
        @PositiveDuration Duration authorizationCodeTimeToLive,
        @PositiveDuration Duration accessTokenTimeToLive,
        @PositiveDuration Duration refreshTokenTimeToLive) {}
