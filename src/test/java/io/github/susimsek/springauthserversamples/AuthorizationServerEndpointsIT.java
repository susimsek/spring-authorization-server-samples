package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

@IntegrationTest
class AuthorizationServerEndpointsIT {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort private int port;

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

    private static JsonNode readJson(String body) throws IOException {
        return OBJECT_MAPPER.readTree(body);
    }

    private static String basicAuth(String username, String password) {
        return Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
