package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import io.github.susimsek.springauthserversamples.session.JpaIndexedSessionRepository;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

@IntegrationTest
class AuthorizationServerEndpointsIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern INPUT_PATTERN =
            Pattern.compile("<input[^>]+name=[\"']([^\"']+)[\"'][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("value=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);

    @LocalServerPort private int port;

    @Autowired private UserSessionRepository userSessionRepository;

    @Autowired private JpaIndexedSessionRepository sessionRepository;

    @Test
    void openIdConfigurationIsPublic() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/.well-known/openid-configuration")))
                                .GET()
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("issuer").contains("token_endpoint");
    }

    @Test
    void clientCredentialsFlowReturnsAccessToken() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/token")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth("demo-client", "demo-secret"))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "grant_type=client_credentials"))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("access_token").contains("Bearer");
    }

    @Test
    void jwkSetIsPublic() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder().uri(URI.create(url("/oauth2/jwks"))).GET().build());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("\"keys\"");
    }

    @Test
    void introspectionReturnsActiveToken() throws IOException, InterruptedException {
        String accessToken = obtainClientCredentialsToken();
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/introspect")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth("demo-client", "demo-secret"))
                                .header("Accept-Language", "tr")
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString("token=" + accessToken))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("\"active\":true");
    }

    @Test
    void userInfoWithoutBearerTokenUsesRfc6750Challenge() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(HttpRequest.newBuilder().uri(URI.create(url("/userinfo"))).GET().build());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .hasValueSatisfying(value -> assertThat(value).startsWith("Bearer"));
    }

    @Test
    void authorizationCodePkceRefreshAndRevocationFlow()
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        String codeVerifier = "integration-test-code-verifier-012345678901234567890123456789";
        String codeChallenge = codeChallenge(codeVerifier);
        String redirectUri = "http://127.0.0.1:8082/callback";

        BrowserSession browser = new BrowserSession();
        URI authorizationUri =
                URI.create(
                        url("/oauth2/authorize")
                                + "?"
                                + formEncode(
                                        Map.of(
                                                "response_type", "code",
                                                "client_id", "pkce-client",
                                                "scope", "openid profile",
                                                "redirect_uri", redirectUri,
                                                "code_challenge", codeChallenge,
                                                "code_challenge_method", "S256",
                                                "state", "integration-state")));

        HttpResponse<String> authorizationResponse = browser.get(authorizationUri);
        assertThat(authorizationResponse.statusCode()).isEqualTo(302);

        URI loginUri = resolveLocation(authorizationUri, authorizationResponse);
        HttpResponse<String> loginResponsePage = browser.get(loginUri);

        URI localizedLoginUri = loginUri;
        if (isRedirect(loginResponsePage)) {
            localizedLoginUri = resolveLocation(loginUri, loginResponsePage);
            assertThat(localizedLoginUri.getPath()).matches("/(en|tr)/login");
        }

        HttpResponse<String> loginPage =
                localizedLoginUri.equals(loginUri)
                        ? loginResponsePage
                        : browser.get(localizedLoginUri);
        assertThat(loginPage.statusCode()).isEqualTo(200);

        Map<String, List<String>> loginForm = parseForm(loginPage.body());
        loginForm.put("username", List.of("admin"));
        loginForm.put("password", List.of("admin"));

        HttpResponse<String> loginResponse = browser.postForm(URI.create(url("/login")), loginForm);
        assertThat(loginResponse.statusCode()).isEqualTo(302);

        var persistedSessions =
                userSessionRepository.findAllByPrincipalNameAndExpiryTimeAfter(
                        "admin", System.currentTimeMillis());
        assertThat(persistedSessions).isNotEmpty();
        assertThat(persistedSessions)
                .allSatisfy(session -> assertThat(session.getAttributes()).isNotEmpty());

        URI postLoginUri = resolveLocation(URI.create(url("/login")), loginResponse);
        HttpResponse<String> postLoginResponse = browser.get(postLoginUri);
        assertThat(postLoginResponse.statusCode()).isEqualTo(302);

        URI consentUri = resolveLocation(postLoginUri, postLoginResponse);
        assertThat(consentUri.getPath()).isEqualTo("/consent");

        HttpResponse<String> consentPage = browser.get(consentUri);
        assertThat(consentPage.statusCode()).isEqualTo(200);
        assertThat(consentPage.body()).contains("Consent");
        assertThat(
                        userSessionRepository.findAllByPrincipalNameAndExpiryTimeAfter(
                                "admin", System.currentTimeMillis()))
                .isNotEmpty();

        Map<String, String> consentParameters = queryParameters(consentUri);
        assertThat(consentParameters.get("client_id")).isEqualTo("pkce-client");
        assertThat(consentParameters.get("state")).isNotBlank();

        Map<String, List<String>> consentForm = new LinkedHashMap<>();
        consentForm.put("client_id", List.of(consentParameters.get("client_id")));
        consentForm.put("state", List.of(consentParameters.get("state")));
        consentForm.put("scope", List.of("openid", "profile"));

        HttpResponse<String> consentResponse =
                browser.postForm(URI.create(url("/oauth2/authorize")), consentForm);
        assertThat(consentResponse.statusCode()).isEqualTo(302);

        URI callbackUri = resolveLocation(URI.create(url("/oauth2/authorize")), consentResponse);
        assertThat(callbackUri.toString()).startsWith(redirectUri);

        Map<String, String> callbackParameters = queryParameters(callbackUri);
        assertThat(callbackParameters.get("state")).isEqualTo("integration-state");
        String authorizationCode = callbackParameters.get("code");
        assertThat(authorizationCode).isNotBlank();

        JsonNode tokenResponse =
                tokenRequest(
                        "pkce-client",
                        "demo-secret",
                        Map.of(
                                "grant_type", "authorization_code",
                                "code", authorizationCode,
                                "redirect_uri", redirectUri,
                                "code_verifier", codeVerifier));

        String accessToken = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.get("refresh_token").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        JsonNode refreshedTokenResponse =
                tokenRequest(
                        "pkce-client",
                        "demo-secret",
                        Map.of("grant_type", "refresh_token", "refresh_token", refreshToken));

        String refreshedAccessToken = refreshedTokenResponse.get("access_token").asText();
        assertThat(refreshedAccessToken).isNotBlank();

        assertThat(
                        introspect("pkce-client", "demo-secret", refreshedAccessToken)
                                .get("active")
                                .asBoolean())
                .isTrue();

        revoke("pkce-client", "demo-secret", refreshedAccessToken);

        assertThat(
                        introspect("pkce-client", "demo-secret", refreshedAccessToken)
                                .get("active")
                                .asBoolean())
                .isFalse();
    }

    @Test
    void expiredJpaSessionIsRemovedByCleanup() {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setPrimaryId(UUID.randomUUID().toString());
        entity.setSessionId(UUID.randomUUID().toString());
        entity.setCreationTime(System.currentTimeMillis() - 120_000);
        entity.setLastAccessTime(System.currentTimeMillis() - 120_000);
        entity.setMaxInactiveInterval(60);
        entity.setExpiryTime(System.currentTimeMillis() - 60_000);
        entity.setPrincipalName("expired-user");
        userSessionRepository.saveAndFlush(entity);

        sessionRepository.cleanUpExpiredSessions();

        assertThat(userSessionRepository.findBySessionId(entity.getSessionId())).isEmpty();
    }

    @Test
    void readinessProbeIsExposed() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/actuator/health/readiness")))
                                .GET()
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static HttpResponse<String> send(HttpRequest request)
            throws IOException, InterruptedException {
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String obtainClientCredentialsToken() throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/token")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth("demo-client", "demo-secret"))
                                .header("Accept-Language", "tr")
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "grant_type=client_credentials"))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        return readJson(response.body()).get("access_token").asText();
    }

    private JsonNode tokenRequest(String clientId, String clientSecret, Map<String, String> form)
            throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/token")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth(clientId, clientSecret))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        return readJson(response.body());
    }

    private JsonNode introspect(String clientId, String clientSecret, String token)
            throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/introspect")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth(clientId, clientSecret))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                formEncode(Map.of("token", token))))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
        return readJson(response.body());
    }

    private void revoke(String clientId, String clientSecret, String token)
            throws IOException, InterruptedException {
        HttpResponse<String> response =
                send(
                        HttpRequest.newBuilder()
                                .uri(URI.create(url("/oauth2/revoke")))
                                .header(
                                        "Authorization",
                                        "Basic " + basicAuth(clientId, clientSecret))
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                formEncode(Map.of("token", token))))
                                .build());

        assertThat(response.statusCode()).isBetween(200, 299);
    }

    private static String codeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
        byte[] digest =
                MessageDigest.getInstance("SHA-256")
                        .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static Map<String, List<String>> parseForm(String html) {
        Map<String, List<String>> form = new LinkedHashMap<>();
        Matcher inputMatcher = INPUT_PATTERN.matcher(html);
        while (inputMatcher.find()) {
            String input = inputMatcher.group();
            String name = inputMatcher.group(1);
            Matcher valueMatcher = VALUE_PATTERN.matcher(input);
            String value = valueMatcher.find() ? valueMatcher.group(1) : "";
            form.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return form;
    }

    private static String formEncode(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String formEncodeMultiValue(Map<String, List<String>> values) {
        return values.entrySet().stream()
                .flatMap(
                        entry ->
                                entry.getValue().stream()
                                        .map(
                                                value ->
                                                        urlEncode(entry.getKey())
                                                                + "="
                                                                + urlEncode(value)))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isRedirect(HttpResponse<?> response) {
        return response.statusCode() >= 300 && response.statusCode() < 400;
    }

    private static URI resolveLocation(URI requestUri, HttpResponse<?> response) {
        String location =
                response.headers()
                        .firstValue("Location")
                        .orElseThrow(() -> new AssertionError("Missing Location header"));
        return requestUri.resolve(location);
    }

    private static Map<String, String> queryParameters(URI uri) {
        if (uri.getRawQuery() == null) {
            return Map.of();
        }
        return Pattern.compile("&")
                .splitAsStream(uri.getRawQuery())
                .map(parameter -> parameter.split("=", 2))
                .collect(
                        Collectors.toMap(
                                parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                                parts ->
                                        parts.length > 1
                                                ? URLDecoder.decode(
                                                        parts[1], StandardCharsets.UTF_8)
                                                : ""));
    }

    private static final class BrowserSession {

        private final CookieManager cookieManager;
        private final HttpClient client;

        private BrowserSession() {
            this.cookieManager = new CookieManager();
            this.cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            this.client =
                    HttpClient.newBuilder()
                            .cookieHandler(cookieManager)
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .build();
        }

        private HttpResponse<String> get(URI uri) throws IOException, InterruptedException {
            return client.send(
                    HttpRequest.newBuilder().uri(uri).header("Accept", "text/html").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        private HttpResponse<String> postForm(URI uri, Map<String, List<String>> form)
                throws IOException, InterruptedException {
            return client.send(
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .header("Accept", "text/html")
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formEncodeMultiValue(form)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }
    }

    private static JsonNode readJson(String body) throws IOException {
        return OBJECT_MAPPER.readTree(body);
    }

    private static String basicAuth(String username, String password) {
        return Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
