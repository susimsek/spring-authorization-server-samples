package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        classes = SpringAuthorizationServerSamplesApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.social-login.encryption-key=http-transport-test-key")
class HttpTransportIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort private int port;

    @Value("${app.authorization-server.issuer}")
    private String configuredIssuer;

    @Test
    void liveHttpExposesDiscoveryReadinessAndClientCredentialsLifecycle() {
        RestClient client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();

        JsonNode discovery =
                client.get()
                        .uri("/.well-known/openid-configuration")
                        .retrieve()
                        .body(JsonNode.class);
        assertThat(discovery).isNotNull();
        assertThat(discovery.path("issuer").asText()).isEqualTo(configuredIssuer);
        assertThat(discovery.path("token_endpoint").asText()).endsWith("/oauth2/token");
        assertThat(discovery.path("jwks_uri").asText()).endsWith("/oauth2/jwks");

        String readiness =
                client.get().uri("/actuator/health/readiness").retrieve().body(String.class);
        assertThat(readiness).contains("\"status\":\"UP\"");

        JsonNode token =
                client.post()
                        .uri("/oauth2/token")
                        .headers(headers -> headers.setBasicAuth("demo-client", "demo-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("grant_type=client_credentials&scope=openid")
                        .retrieve()
                        .body(JsonNode.class);
        assertThat(token).isNotNull();
        String accessToken = token.path("access_token").asText();
        assertThat(accessToken).isNotBlank();

        JsonNode active =
                client.post()
                        .uri("/oauth2/introspect")
                        .headers(headers -> headers.setBasicAuth("demo-client", "demo-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("token=" + accessToken)
                        .retrieve()
                        .body(JsonNode.class);
        assertThat(active).isNotNull();
        assertThat(active.path("active").asBoolean()).isTrue();

        client.post()
                .uri("/oauth2/revoke")
                .headers(headers -> headers.setBasicAuth("demo-client", "demo-secret"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("token=" + accessToken)
                .retrieve()
                .toBodilessEntity();

        JsonNode inactive =
                client.post()
                        .uri("/oauth2/introspect")
                        .headers(headers -> headers.setBasicAuth("demo-client", "demo-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("token=" + accessToken)
                        .retrieve()
                        .body(JsonNode.class);
        assertThat(inactive).isNotNull();
        assertThat(inactive.path("active").asBoolean()).isFalse();
    }

    @Test
    void liveHttpRejectsAnonymousConsoleApiRequestsAtTheSecurityFilter() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> accountResponse =
                client.send(request("/api/account/mfa"), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> adminResponse =
                client.send(request("/api/admin/users"), HttpResponse.BodyHandlers.ofString());

        assertThat(accountResponse.statusCode()).isEqualTo(401);
        assertThat(accountResponse.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("Bearer"));
        assertThat(adminResponse.statusCode()).isEqualTo(401);
        assertThat(adminResponse.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("Bearer"));
    }

    @Test
    void liveHttpPkceTokensEnforceAdminAndAccountApiBoundaries() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);

        String adminAccessToken =
                accessToken(client, "admin-console", "admin-api", "admin-http-transport-verifier");
        HttpResponse<String> adminWhoAmI =
                client.send(
                        apiRequest("/api/admin/whoami", adminAccessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(adminWhoAmI.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(adminWhoAmI.body()).path("username").asText()).isEqualTo("admin");

        HttpResponse<String> cappedUsers =
                client.send(
                        apiRequest("/api/admin/users?page=0&size=1000", adminAccessToken)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(cappedUsers.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(cappedUsers.body()).path("page").path("size").asInt())
                .isEqualTo(100);

        String accountAccessToken =
                accessToken(
                        client,
                        "account-console",
                        "account-api",
                        "account-http-transport-verifier");
        HttpResponse<String> accountMfa =
                client.send(
                        apiRequest("/api/account/mfa", accountAccessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(accountMfa.statusCode()).isEqualTo(200);

        HttpResponse<String> accountDeniedFromAdminApi =
                client.send(
                        apiRequest("/api/account/mfa", adminAccessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> adminDeniedFromAccountApi =
                client.send(
                        apiRequest("/api/admin/whoami", accountAccessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(accountDeniedFromAdminApi.statusCode()).isEqualTo(403);
        assertThat(adminDeniedFromAccountApi.statusCode()).isEqualTo(403);
    }

    @Test
    void liveHttpAccountValidationReturnsLocalizedProblemDetail() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);
        String accessToken =
                accessToken(
                        client,
                        "account-console",
                        "account-api",
                        "account-validation-http-transport-verifier");

        HttpResponse<String> response =
                client.send(
                        apiRequest("/api/account/password", accessToken)
                                .header("Accept-Language", "tr")
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .PUT(
                                        HttpRequest.BodyPublishers.ofString(
                                                "{\"currentPassword\":\"wrong\",\"newPassword\":\"Valid-new12!\"}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode problem = JSON.readTree(response.body());
        assertThat(problem.path("errorCode").asText()).isEqualTo("invalid_current_password");
        assertThat(problem.path("field").asText()).isEqualTo("currentPassword");
        assertThat(problem.path("detail").asText()).isEqualTo("Mevcut parola geçersiz.");
    }

    @Test
    void liveHttpAdminCrudAndOperationalEndpointsUseTheRealSecurityChain() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);
        String accessToken =
                accessToken(
                        client, "admin-console", "admin-api", "admin-crud-http-transport-verifier");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String clientId = "http-it-" + suffix;
        String providerRegistration = "http" + suffix.substring(0, 12);
        String createdClientId = null;
        String providerId = null;
        String mapperId = null;
        Long localizationId = null;
        try {
            HttpResponse<String> createdClient =
                    client.send(
                            jsonRequest("/api/admin/clients", accessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    clientRequest(clientId, "HTTP IT Client")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(createdClient.statusCode()).isEqualTo(201);
            JsonNode clientBody = JSON.readTree(createdClient.body());
            createdClientId = clientBody.path("client").path("id").asText();
            assertThat(clientBody.path("client").path("clientId").asText()).isEqualTo(clientId);
            assertThat(clientBody.path("clientSecret").asText()).isNotBlank();

            HttpResponse<String> updatedClient =
                    client.send(
                            jsonRequest("/api/admin/clients/" + createdClientId, accessToken)
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    clientRequest(
                                                            clientId, "Updated HTTP IT Client")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(updatedClient.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(updatedClient.body()).path("clientName").asText())
                    .isEqualTo("Updated HTTP IT Client");

            HttpResponse<String> rotatedSecret =
                    client.send(
                            apiRequest(
                                            "/api/admin/clients/" + createdClientId + "/secret",
                                            accessToken)
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(rotatedSecret.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(rotatedSecret.body()).path("clientSecret").asText())
                    .isNotBlank();

            HttpResponse<String> createdProvider =
                    client.send(
                            jsonRequest("/api/admin/identity-providers", accessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    identityProviderRequest(
                                                            providerRegistration, "HTTP IT OIDC")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(createdProvider.statusCode()).isEqualTo(201);
            providerId = JSON.readTree(createdProvider.body()).path("id").asText();
            assertThat(providerId).isNotBlank();

            HttpResponse<String> createdMapper =
                    client.send(
                            jsonRequest(
                                            "/api/admin/identity-providers/"
                                                    + providerId
                                                    + "/mappers",
                                            accessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    mapperRequest("http-email")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(createdMapper.statusCode()).isEqualTo(200);
            mapperId = JSON.readTree(createdMapper.body()).path("id").asText();
            assertThat(mapperId).isNotBlank();

            HttpResponse<String> createdLocalization =
                    client.send(
                            jsonRequest("/api/admin/settings/localization/messages", accessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    localizationRequest(
                                                            "http.test." + suffix,
                                                            "HTTP test message")))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(createdLocalization.statusCode()).isEqualTo(201);
            localizationId = JSON.readTree(createdLocalization.body()).path("id").asLong();

            HttpResponse<String> requiredActions =
                    client.send(
                            apiRequest("/api/admin/required-actions", accessToken).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> rotatedKey =
                    client.send(
                            apiRequest("/api/admin/keys/rotate", accessToken)
                                    .POST(HttpRequest.BodyPublishers.noBody())
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(requiredActions.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(requiredActions.body()).isArray()).isTrue();
            assertThat(rotatedKey.statusCode()).isEqualTo(200);
            JsonNode key = JSON.readTree(rotatedKey.body());
            assertThat(key.path("active").asBoolean()).isTrue();
            assertThat(key.path("type").asText()).isEqualTo("RSA");
            assertThat(key.path("use").asText()).isEqualTo("sig");
            assertThat(key.path("algorithm").asText()).isEqualTo("RS256");
        } finally {
            if (mapperId != null && providerId != null) {
                delete(
                        client,
                        "/api/admin/identity-providers/" + providerId + "/mappers/" + mapperId,
                        accessToken);
            }
            if (providerId != null) {
                delete(client, "/api/admin/identity-providers/" + providerId, accessToken);
            }
            if (localizationId != null) {
                delete(
                        client,
                        "/api/admin/settings/localization/messages/" + localizationId,
                        accessToken);
            }
            if (createdClientId != null) {
                delete(client, "/api/admin/clients/" + createdClientId, accessToken);
            }
        }
    }

    @Test
    void liveHttpAdminUserLifecycleUsesRealSerializationAndCleansUp() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);
        String accessToken =
                accessToken(
                        client, "admin-console", "admin-api", "admin-user-http-transport-verifier");
        String username = "http-user-" + UUID.randomUUID().toString().replace("-", "");
        Long userId = null;
        try {
            HttpResponse<String> created =
                    client.send(
                            jsonRequest("/api/admin/users", accessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"username\":\""
                                                            + username
                                                            + "\",\"password\":\"Http-test12!\",\"enabled\":true,\"roles\":[\"ROLE_USER\"]}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(created.statusCode()).isEqualTo(201);
            userId = JSON.readTree(created.body()).path("id").asLong();
            assertThat(userId).isPositive();

            HttpResponse<String> disabled =
                    client.send(
                            jsonRequest("/api/admin/users/" + userId + "/enabled", accessToken)
                                    .PUT(HttpRequest.BodyPublishers.ofString("{\"enabled\":false}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(disabled.statusCode()).isEqualTo(204);

            HttpResponse<String> enabled =
                    client.send(
                            jsonRequest("/api/admin/users/" + userId + "/enabled", accessToken)
                                    .PUT(HttpRequest.BodyPublishers.ofString("{\"enabled\":true}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(enabled.statusCode()).isEqualTo(204);

            HttpResponse<String> password =
                    client.send(
                            jsonRequest("/api/admin/users/" + userId + "/password", accessToken)
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"password\":\"Http-new123!\",\"temporary\":true}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(password.statusCode()).withFailMessage(password.body()).isEqualTo(204);

        } finally {
            if (userId != null) {
                delete(client, "/api/admin/users/" + userId, accessToken);
            }
        }
    }

    @Test
    void liveHttpAccountSecurityReadsMfaRecoveryPasskeysProfileAndSocialLinks() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);
        String accessToken =
                accessToken(
                        client,
                        "account-console",
                        "account-api",
                        "account-security-http-transport-verifier");

        Map<String, String> endpoints =
                Map.of(
                        "/api/account/mfa", "enabled",
                        "/api/account/mfa/recovery-codes", "remaining",
                        "/api/account/webauthn/credentials?size=1000", "content",
                        "/api/account/profile/attributes", "attributes");
        for (Map.Entry<String, String> endpoint : endpoints.entrySet()) {
            HttpResponse<String> response =
                    client.send(
                            apiRequest(endpoint.getKey(), accessToken).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(response.body()).path(endpoint.getValue()).isMissingNode())
                    .isFalse();
        }

        HttpResponse<String> socialLinks =
                client.send(
                        apiRequest("/api/account/social-links", accessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(socialLinks.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(socialLinks.body()).isArray()).isTrue();
    }

    @Test
    void liveHttpAccountProfileMutationRestoresTheOriginalAttributes() throws Exception {
        HttpClient client = authenticatedClient();
        login(client);
        String accessToken =
                accessToken(
                        client,
                        "account-console",
                        "account-api",
                        "account-profile-http-transport-verifier");

        HttpResponse<String> before =
                client.send(
                        apiRequest("/api/account/profile/attributes", accessToken).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(before.statusCode()).isEqualTo(200);
        String originalAttributes = JSON.readTree(before.body()).path("attributes").toString();
        try {
            HttpResponse<String> updated =
                    client.send(
                            jsonRequest("/api/account/profile/attributes", accessToken)
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"attributes\":{\"department\":[\""
                                                            + UUID.randomUUID()
                                                            + "\"]}}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(updated.statusCode()).isEqualTo(200);
            assertThat(
                            JSON.readTree(updated.body())
                                    .path("attributes")
                                    .path("department")
                                    .isArray())
                    .isTrue();
        } finally {
            HttpResponse<String> restored =
                    client.send(
                            jsonRequest("/api/account/profile/attributes", accessToken)
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"attributes\":" + originalAttributes + "}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(restored.statusCode()).isEqualTo(200);
        }
    }

    @Test
    void liveHttpAccountPasswordChangeAndDeletionUseTheRealSecurityChain() throws Exception {
        HttpClient adminClient = authenticatedClient();
        login(adminClient);
        String adminAccessToken =
                accessToken(
                        adminClient,
                        "admin-console",
                        "admin-api",
                        "account-password-http-transport-admin-verifier");
        String username = "http-account-" + UUID.randomUUID().toString().replace("-", "");
        Long userId = null;
        boolean deleted = false;
        try {
            HttpResponse<String> created =
                    adminClient.send(
                            jsonRequest("/api/admin/users", adminAccessToken)
                                    .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"username\":\""
                                                            + username
                                                            + "\",\"password\":\"Http-current12!\",\"enabled\":true,\"roles\":[\"ROLE_USER\"]}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(created.statusCode()).isEqualTo(201);
            userId = JSON.readTree(created.body()).path("id").asLong();
            assertThat(userId).isPositive();

            HttpClient accountClient = authenticatedClient();
            login(accountClient, username, "Http-current12!");
            String accountAccessToken =
                    accessToken(
                            accountClient,
                            "account-console",
                            "account-api",
                            "account-password-http-transport-account-verifier");

            HttpResponse<String> changed =
                    accountClient.send(
                            jsonRequest("/api/account/password", accountAccessToken)
                                    .PUT(
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"currentPassword\":\"Http-current12!\",\"newPassword\":\"Http-changed12!\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(changed.statusCode()).isEqualTo(204);

            HttpResponse<String> deletedResponse =
                    accountClient.send(
                            jsonRequest("/api/account", accountAccessToken)
                                    .method(
                                            "DELETE",
                                            HttpRequest.BodyPublishers.ofString(
                                                    "{\"currentPassword\":\"Http-changed12!\"}"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(deletedResponse.statusCode()).isEqualTo(204);
            deleted = true;
        } finally {
            if (!deleted && userId != null) {
                delete(adminClient, "/api/admin/users/" + userId, adminAccessToken);
            }
        }
    }

    private HttpClient authenticatedClient() {
        return HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private void login(HttpClient client) throws Exception {
        login(client, "admin", "admin");
    }

    private void login(HttpClient client, String username, String password) throws Exception {
        HttpResponse<String> response =
                client.send(
                        requestBuilder("/login")
                                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                form(
                                                        Map.of(
                                                                "username",
                                                                username,
                                                                "password",
                                                                password))))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(300, 399);
    }

    private String accessToken(HttpClient client, String clientId, String apiScope, String verifier)
            throws Exception {
        String redirectUri =
                "http://localhost:9090/"
                        + ("admin-api".equals(apiScope) ? "admin" : "account")
                        + "/callback";
        Map<String, String> authorization = new LinkedHashMap<>();
        authorization.put("response_type", "code");
        authorization.put("client_id", clientId);
        authorization.put("scope", "openid profile " + apiScope);
        authorization.put("redirect_uri", redirectUri);
        authorization.put("code_challenge", codeChallenge(verifier));
        authorization.put("code_challenge_method", "S256");
        authorization.put("state", "http-transport-state");
        authorization.put("nonce", "http-transport-nonce");
        HttpResponse<String> authorize =
                client.send(
                        requestBuilder("/oauth2/authorize?" + form(authorization))
                                .header("Accept", MediaType.TEXT_HTML_VALUE)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(authorize.statusCode()).isBetween(300, 399);
        String location = authorize.headers().firstValue("Location").orElseThrow();
        String authorizationCode =
                UriComponentsBuilder.fromUri(URI.create(location))
                        .build()
                        .getQueryParams()
                        .getFirst("code");
        assertThat(authorizationCode).isNotBlank();

        Map<String, String> token = new LinkedHashMap<>();
        token.put("client_id", clientId);
        token.put("grant_type", "authorization_code");
        token.put("code", authorizationCode);
        token.put("redirect_uri", redirectUri);
        token.put("code_verifier", verifier);
        HttpResponse<String> tokenResponse =
                client.send(
                        requestBuilder("/oauth2/token")
                                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                                .POST(HttpRequest.BodyPublishers.ofString(form(token)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(tokenResponse.statusCode()).isEqualTo(200);
        String accessToken = JSON.readTree(tokenResponse.body()).path("access_token").asText();
        assertThat(accessToken).isNotBlank();
        return accessToken;
    }

    private HttpRequest.Builder apiRequest(String path, String accessToken) {
        return requestBuilder(path).header("Authorization", "Bearer " + accessToken);
    }

    private HttpRequest.Builder jsonRequest(String path, String accessToken) {
        return apiRequest(path, accessToken)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    }

    private static String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String codeChallenge(String verifier) throws Exception {
        byte[] digest =
                MessageDigest.getInstance("SHA-256")
                        .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String clientRequest(String clientId, String clientName) {
        return """
        {"clientId":"%s","clientName":"%s","clientAuthenticationMethods":["client_secret_basic"],"authorizationGrantTypes":["client_credentials"],"redirectUris":[],"postLogoutRedirectUris":[],"scopes":["openid"],"requireAuthorizationConsent":false,"requireProofKey":false,"requireDpop":false,"requireDpopJkt":false,"dpopRefreshTokenOnly":false,"dpopSigningAlgorithms":["RS256","ES256"],"authorizationCodeTimeToLive":"PT5M","accessTokenTimeToLive":"PT5M","refreshTokenTimeToLive":"PT1H"}
        """
                .formatted(clientId, clientName);
    }

    private static String identityProviderRequest(String registrationId, String displayName) {
        return """
        {"registrationId":"%1$s","providerType":"oidc","displayName":"%2$s","alias":"%1$s","iconKey":"generic","shortStateParameter":false,"caseSensitiveUsername":false,"enabled":false,"clientId":"http-it-client","clientSecret":"http-it-secret","hideOnLogin":false,"accountLinkingOnly":false,"trustEmail":false,"mfaRequired":false,"requiredClaims":"sub","storeTokens":false,"storedTokensReadable":false,"guiOrder":99,"showInAccountConsole":"always","syncMode":"import","authorizationUri":"https://idp.example.test/authorize","tokenUri":"https://idp.example.test/token","userInfoUri":null,"jwkSetUri":null,"issuerUri":null,"clientAuthenticationMethod":"client_secret_basic","scopes":"openid,profile","userNameAttribute":"sub"}
        """
                .formatted(registrationId, displayName);
    }

    private static String mapperRequest(String name) {
        return """
        {"name":"%s","sourceClaim":"email","target":"email","mapperType":"user-attribute","syncMode":"inherit","addToIdToken":true,"addToAccessToken":true}
        """
                .formatted(name);
    }

    private static String localizationRequest(String messageKey, String messageValue) {
        return """
        {"locale":"tr","bundle":"admin","messageKey":"%s","messageValue":"%s"}
        """
                .formatted(messageKey, messageValue);
    }

    private void delete(HttpClient client, String path, String accessToken) throws Exception {
        HttpResponse<String> response =
                client.send(
                        apiRequest(path, accessToken).DELETE().build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(204);
    }

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build();
    }
}
