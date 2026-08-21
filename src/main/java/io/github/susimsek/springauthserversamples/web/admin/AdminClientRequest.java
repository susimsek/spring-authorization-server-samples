package io.github.susimsek.springauthserversamples.web.admin;

import io.github.susimsek.springauthserversamples.web.admin.validation.AbsoluteUri;
import io.github.susimsek.springauthserversamples.web.admin.validation.PositiveDuration;
import io.github.susimsek.springauthserversamples.web.admin.validation.ValidAdminClientConfiguration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.Set;

@ValidAdminClientConfiguration
public record AdminClientRequest(
        @NotBlank(message = "{admin.validation.required}") String clientId,
        @NotBlank(message = "{admin.validation.required}") String clientName,
        @NotEmpty(message = "{admin.validation.selection}")
                Set<@NotBlank(message = "{admin.validation.selection}") String>
                        clientAuthenticationMethods,
        @NotEmpty(message = "{admin.validation.selection}")
                Set<@NotBlank(message = "{admin.validation.selection}") String>
                        authorizationGrantTypes,
        Set<@AbsoluteUri String> redirectUris,
        Set<@AbsoluteUri String> postLogoutRedirectUris,
        @NotEmpty(message = "{admin.validation.scope}")
                Set<@NotBlank(message = "{admin.validation.scope}") String> scopes,
        boolean requireAuthorizationConsent,
        boolean requireProofKey,
        @PositiveDuration Duration authorizationCodeTimeToLive,
        @PositiveDuration Duration accessTokenTimeToLive,
        @PositiveDuration Duration refreshTokenTimeToLive) {}
