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
    void openApiGroupsExposeDocumentedControllerContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs/admin-api"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/admin/clients'].get.summary")
                                .value("Search clients"))
                .andExpect(
                        jsonPath("$.paths['/api/admin/clients'].get.security[0].adminBearer")
                                .exists());

        mockMvc.perform(get("/v3/api-docs/account-api"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/api/account/profile'].get.summary")
                                .value("Read profile"))
                .andExpect(
                        jsonPath("$.paths['/api/account/profile'].get.security[0].accountBearer")
                                .exists());

        mockMvc.perform(get("/v3/api-docs/oauth2-oidc"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.paths['/oauth2/token'].post.summary").value("Token Endpoint"))
                .andExpect(
                        jsonPath("$.paths['/oidc/session-status'].get.summary")
                                .value("Get browser SSO session status"));
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

    @Test
    void adminConsoleUsesPkceWithoutConsent() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("username", "admin")
                                        .param("password", "admin"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        String codeVerifier = "admin-console-code-verifier-0123456789012345678901234567890";
        String redirectUri = "http://localhost:9090/en/admin/callback";

        MvcResult result =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .cookie(sessionCookie)
                                        .accept(MediaType.TEXT_HTML)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", "admin-console")
                                        .queryParam("scope", "openid profile admin-api")
                                        .queryParam("redirect_uri", redirectUri)
                                        .queryParam("code_challenge", codeChallenge(codeVerifier))
                                        .queryParam("code_challenge_method", "S256")
                                        .queryParam("state", "admin-console-state")
                                        .queryParam("nonce", "admin-console-nonce"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        URI callbackUri = URI.create(result.getResponse().getRedirectedUrl());
        assertThat(callbackUri.getPath()).isEqualTo("/en/admin/callback");
        assertThat(callbackUri.getQuery()).contains("code=");
        assertThat(callbackUri.getQuery()).contains("state=admin-console-state");

        String authorizationCode =
                UriComponentsBuilder.fromUri(callbackUri).build().getQueryParams().getFirst("code");
        MvcResult tokenResult =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("client_id", "admin-console")
                                        .param("code", authorizationCode)
                                        .param("code_verifier", codeVerifier)
                                        .param("grant_type", "authorization_code")
                                        .param("redirect_uri", redirectUri))
                        .andExpect(status().isOk())
                        .andReturn();

        JsonNode tokenResponse =
                JSON_MAPPER.readTree(tokenResult.getResponse().getContentAsString());
        String accessToken = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.get("refresh_token").asText();
        String idToken = tokenResponse.get("id_token").asText();
        assertThat(refreshToken).isNotBlank();
        assertThat(idToken).isNotBlank();
        assertThat(jwtClaims(idToken).get("nonce").asText()).isEqualTo("admin-console-nonce");
        assertThat(jwtClaims(accessToken).get("picture").asText())
                .matches("http://127\\.0\\.0\\.1:\\d+/avatars/[a-f0-9-]{36}\\?v=\\d+");
        mockMvc.perform(get("/api/admin/whoami").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.access.manageClients").value(true));

        MvcResult refreshResult =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("client_id", "admin-console")
                                        .param("grant_type", "refresh_token")
                                        .param("refresh_token", refreshToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.access_token").isNotEmpty())
                        .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                        .andExpect(jsonPath("$.id_token").isNotEmpty())
                        .andReturn();

        String rotatedRefreshToken =
                JSON_MAPPER
                        .readTree(refreshResult.getResponse().getContentAsString())
                        .get("refresh_token")
                        .asText();
        mockMvc.perform(
                        post("/oauth2/revoke")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("client_id", "admin-console")
                                .param("token", rotatedRefreshToken)
                                .param("token_type_hint", "refresh_token"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/oauth2/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("client_id", "admin-console")
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", rotatedRefreshToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminConsoleOidcLogoutUsesIdTokenHint() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("username", "admin")
                                        .param("password", "admin"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        String codeVerifier = "admin-console-logout-verifier-012345678901234567890123456789012";
        String redirectUri = "http://localhost:9090/en/admin/callback";
        MvcResult authorizeResult =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .cookie(sessionCookie)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", "admin-console")
                                        .queryParam("scope", "openid profile admin-api")
                                        .queryParam("redirect_uri", redirectUri)
                                        .queryParam("code_challenge", codeChallenge(codeVerifier))
                                        .queryParam("code_challenge_method", "S256")
                                        .queryParam("state", "logout-state")
                                        .queryParam("nonce", "logout-nonce"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        URI callbackUri = URI.create(authorizeResult.getResponse().getRedirectedUrl());
        String authorizationCode =
                UriComponentsBuilder.fromUri(callbackUri).build().getQueryParams().getFirst("code");
        MvcResult tokenResult =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("client_id", "admin-console")
                                        .param("code", authorizationCode)
                                        .param("code_verifier", codeVerifier)
                                        .param("grant_type", "authorization_code")
                                        .param("redirect_uri", redirectUri))
                        .andExpect(status().isOk())
                        .andReturn();
        String idToken =
                JSON_MAPPER
                        .readTree(tokenResult.getResponse().getContentAsString())
                        .get("id_token")
                        .asText();

        mockMvc.perform(
                        get("/connect/logout")
                                .cookie(sessionCookie)
                                .queryParam("client_id", "admin-console")
                                .queryParam("id_token_hint", idToken)
                                .queryParam(
                                        "post_logout_redirect_uri",
                                        "http://localhost:9090/en/admin/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        logoutResult ->
                                assertThat(logoutResult.getResponse().getRedirectedUrl())
                                        .isEqualTo("http://localhost:9090/en/admin/"));
    }

    @Test
    void adminConsoleSilentAuthorizationReusesAuthenticatedSession() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("username", "admin")
                                        .param("password", "admin"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        String codeVerifier = "admin-console-silent-code-verifier-0123456789012345678901234567890";
        String redirectUri = "http://localhost:9090/en/admin/callback";

        MvcResult result =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .cookie(sessionCookie)
                                        .accept(MediaType.TEXT_HTML)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", "admin-console")
                                        .queryParam("scope", "openid profile admin-api")
                                        .queryParam("redirect_uri", redirectUri)
                                        .queryParam("code_challenge", codeChallenge(codeVerifier))
                                        .queryParam("code_challenge_method", "S256")
                                        .queryParam("prompt", "none")
                                        .queryParam("state", "reload-state")
                                        .queryParam("nonce", "reload-nonce"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        URI callbackUri = URI.create(result.getResponse().getRedirectedUrl());
        assertThat(callbackUri.getPath()).isEqualTo("/en/admin/callback");
        assertThat(callbackUri.getQuery()).contains("code=");
        assertThat(callbackUri.getQuery()).contains("state=reload-state");
    }

    @Test
    void adminAndAccountConsolesReuseTheSameSsoSession() throws Exception {
        MvcResult loginResult =
                mockMvc.perform(
                                post("/login")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("username", "admin")
                                        .param("password", "admin"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var sessionCookie = loginResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        assertConsoleAuthorizationRedirectsWithoutLogin(
                sessionCookie,
                "admin-console",
                "openid profile admin-api",
                "http://localhost:9090/en/admin/callback",
                "shared-sso-admin-state",
                "shared-sso-admin-nonce",
                "shared-sso-admin-verifier-012345678901234567890123456789012");
        assertConsoleAuthorizationRedirectsWithoutLogin(
                sessionCookie,
                "account-console",
                "openid profile account-api",
                "http://localhost:9090/en/account/callback",
                "shared-sso-account-state",
                "shared-sso-account-nonce",
                "shared-sso-account-verifier-0123456789012345678901234567890");
    }

    private void assertConsoleAuthorizationRedirectsWithoutLogin(
            jakarta.servlet.http.Cookie sessionCookie,
            String clientId,
            String scope,
            String redirectUri,
            String state,
            String nonce,
            String codeVerifier)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .cookie(sessionCookie)
                                        .accept(MediaType.TEXT_HTML)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", clientId)
                                        .queryParam("scope", scope)
                                        .queryParam("redirect_uri", redirectUri)
                                        .queryParam("code_challenge", codeChallenge(codeVerifier))
                                        .queryParam("code_challenge_method", "S256")
                                        .queryParam("state", state)
                                        .queryParam("nonce", nonce))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();

        URI callbackUri = URI.create(result.getResponse().getRedirectedUrl());
        assertThat(callbackUri.toString()).startsWith(redirectUri);
        assertThat(callbackUri.getQuery()).contains("code=");
        assertThat(callbackUri.getQuery()).contains("state=" + state);
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

    private static JsonNode jwtClaims(String token) throws Exception {
        String encodedClaims = token.split("\\.")[1];
        return JSON_MAPPER.readTree(Base64.getUrlDecoder().decode(encodedClaims));
    }
}
