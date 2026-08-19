package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
class AuthorizationServerEndpointsIT {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Autowired private MockMvc mockMvc;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private OAuth2AuthorizationConsentService authorizationConsentService;

    @Test
    void discoveryAndJwkEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.token_endpoint").exists());

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    void clientCredentialsTokenCanBeIntrospectedAndRevoked() throws Exception {
        String accessToken =
                tokenRequest(
                                "demo-client",
                                "demo-secret",
                                Map.of("grant_type", "client_credentials"))
                        .get("access_token")
                        .asText();

        assertThat(accessToken).isNotBlank();

        mockMvc.perform(
                        post("/oauth2/introspect")
                                .with(httpBasic("demo-client", "demo-secret"))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(
                        post("/oauth2/revoke")
                                .with(httpBasic("demo-client", "demo-secret"))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("token", accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/oauth2/introspect")
                                .with(httpBasic("demo-client", "demo-secret"))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void consentApiSeparatesPreviouslyApprovedScopes() throws Exception {
        RegisteredClient client = registeredClientRepository.findByClientId("pkce-client");
        assertThat(client).isNotNull();

        OAuth2AuthorizationConsent existingConsent =
                OAuth2AuthorizationConsent.withId(client.getId(), "admin").scope("profile").build();
        authorizationConsentService.save(existingConsent);

        try {
            mockMvc.perform(
                            get("/api/authorization/consent")
                                    .with(user("admin").roles("ADMIN"))
                                    .queryParam("client_id", "pkce-client")
                                    .queryParam("scope", "openid profile")
                                    .queryParam("state", "consent-state"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopes").isEmpty())
                    .andExpect(jsonPath("$.previouslyApprovedScopes[0]").value("profile"));
        } finally {
            authorizationConsentService.remove(existingConsent);
        }
    }

    @Test
    void authorizationCodePkceRequestRedirectsToConsent() throws Exception {
        String codeVerifier = "integration-test-code-verifier-012345678901234567890123456789";
        String redirectUri = "http://127.0.0.1:8082/callback";

        MvcResult result =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .with(user("admin").roles("ADMIN"))
                                        .accept(MediaType.TEXT_HTML)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", "pkce-client")
                                        .queryParam("scope", "openid profile")
                                        .queryParam("redirect_uri", redirectUri)
                                        .queryParam("code_challenge", codeChallenge(codeVerifier))
                                        .queryParam("code_challenge_method", "S256")
                                        .queryParam("state", "integration-state"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        URI consentUri = URI.create(result.getResponse().getRedirectedUrl());
        assertThat(consentUri.getPath()).isEqualTo("/consent");

        Map<String, String> consentParameters =
                UriComponentsBuilder.fromUri(consentUri)
                        .build()
                        .getQueryParams()
                        .toSingleValueMap();

        assertThat(consentParameters.get("client_id")).isEqualTo("pkce-client");
        assertThat(consentParameters.get("scope")).contains("openid", "profile");
        assertThat(consentParameters.get("state")).isNotBlank();
    }

    private JsonNode tokenRequest(String clientId, String clientSecret, Map<String, String> form)
            throws Exception {
        var request =
                post("/oauth2/token")
                        .with(httpBasic(clientId, clientSecret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED);

        form.forEach(request::param);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return JSON_MAPPER.readTree(result.getResponse().getContentAsString());
    }

    private static String codeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        byte[] digest =
                MessageDigest.getInstance("SHA-256")
                        .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
