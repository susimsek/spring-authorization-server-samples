package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

class MicrosoftOidcIdTokenValidatorTest {

    private static final String CLIENT_ID = "microsoft-client";
    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void acceptsTenantSpecificMicrosoftIssuer() {
        assertThat(validator().validate(token("tenant-id")).hasErrors()).isFalse();
    }

    @Test
    void rejectsNonMicrosoftIssuer() {
        assertThat(validator().validate(token("common")).hasErrors()).isTrue();
    }

    private static MicrosoftOidcIdTokenValidator validator() {
        return new MicrosoftOidcIdTokenValidator(
                ClientRegistration.withRegistrationId("microsoft")
                        .clientId(CLIENT_ID)
                        .clientSecret("secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationUri("https://login.microsoftonline.com/common/authorize")
                        .tokenUri("https://login.microsoftonline.com/common/token")
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .build());
    }

    private static Jwt token(String tenant) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim(
                        IdTokenClaimNames.ISS,
                        "https://login.microsoftonline.com/" + tenant + "/v2.0")
                .claim(IdTokenClaimNames.SUB, "subject")
                .claim(IdTokenClaimNames.AUD, CLIENT_ID)
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .build();
    }
}
