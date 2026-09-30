package gatling;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

public final class GatlingDefaults {

    private GatlingDefaults() {}

    public static String host() {
        return Optional.ofNullable(System.getProperty("httpHost")).orElse("127.0.0.1");
    }

    public static int port() {
        return Integer.getInteger("httpPort", 9090);
    }

    public static String baseUrl() {
        return "http://" + host() + ":" + port();
    }

    public static int users() {
        return Integer.getInteger("users", 5);
    }

    public static Duration rampDuration() {
        return Duration.ofMinutes(Integer.getInteger("ramp", 1));
    }

    public static Duration testDuration() {
        return Duration.ofMinutes(Integer.getInteger("duration", 1));
    }

    public static Duration maxDuration() {
        return rampDuration().plus(testDuration()).plusSeconds(30);
    }

    public static double maxFailurePercentage() {
        return Double.parseDouble(System.getProperty("maxFailurePercentage", "1.0"));
    }

    public static int maxResponseTimeMillis() {
        return Integer.getInteger("maxResponseTimeMillis", 2_000);
    }

    public static int maxP99ResponseTimeMillis() {
        return Integer.getInteger("maxP99ResponseTimeMillis", 3_000);
    }

    public static Duration minPause() {
        return Duration.ofSeconds(Long.getLong("minPauseSeconds", 5));
    }

    public static Duration maxPause() {
        return Duration.ofSeconds(Long.getLong("maxPauseSeconds", 10));
    }

    public static Duration pause() {
        return Duration.ofSeconds(Long.getLong("pauseSeconds", 5));
    }

    public static String locale() {
        return Optional.ofNullable(System.getProperty("locale")).orElse("tr");
    }

    public static String clientId() {
        return Optional.ofNullable(System.getProperty("clientId")).orElse("demo-client");
    }

    public static String clientSecret() {
        return Optional.ofNullable(System.getProperty("clientSecret")).orElse("demo-secret");
    }

    public static String scope() {
        return Optional.ofNullable(System.getProperty("scope")).orElse("openid");
    }

    public static String accountUsername() {
        return Optional.ofNullable(System.getProperty("accountUsername")).orElse("user");
    }

    public static String accountPassword() {
        return Optional.ofNullable(System.getProperty("accountPassword")).orElse("user");
    }

    public static String pkceCodeVerifier() {
        return Optional.ofNullable(System.getProperty("pkceCodeVerifier"))
                .orElse("gatling-account-code-verifier-012345678901234567890123456789");
    }

    public static String pkceCodeChallenge() {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(pkceCodeVerifier().getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available for PKCE", exception);
        }
    }

    public static String basicAuthorizationValue() {
        return basicAuthorizationValue(clientId(), clientSecret());
    }

    public static String basicAuthorizationValue(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        String encoded =
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    public static HttpProtocolBuilder httpProtocol() {
        return http.baseUrl(baseUrl())
                .disableFollowRedirect()
                .acceptHeader("application/json")
                .acceptEncodingHeader("gzip, deflate")
                .acceptLanguageHeader(locale())
                .contentTypeHeader("application/x-www-form-urlencoded")
                .userAgentHeader("Gatling OAuth2 Performance Test");
    }
}
