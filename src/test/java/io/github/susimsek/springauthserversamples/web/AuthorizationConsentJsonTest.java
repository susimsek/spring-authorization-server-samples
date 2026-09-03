package io.github.susimsek.springauthserversamples.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springauthserversamples.dto.oauth.AuthorizationConsentDTO;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AuthorizationConsentJsonTest {

    @Test
    void serializesConsentScopesWithoutSecurityTypeMetadata() throws Exception {
        var view =
                new AuthorizationConsentDTO(
                        "pkce-client",
                        "state",
                        new LinkedHashSet<>(java.util.List.of("profile")),
                        new LinkedHashSet<>(),
                        "admin",
                        null,
                        "/oauth2/authorize");

        String json = JsonMapper.builder().build().writeValueAsString(view);

        assertThat(json).contains("\"scopes\":[\"profile\"]");
        assertThat(json).contains("\"previouslyApprovedScopes\":[]");
        assertThat(json).doesNotContain("java.util.LinkedHashSet");
    }
}
