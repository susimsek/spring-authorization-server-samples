package gatling;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.nio.charset.StandardCharsets;
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

    public static String basicAuthorizationValue() {
        String credentials = clientId() + ":" + clientSecret();
        String encoded =
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    public static HttpProtocolBuilder httpProtocol() {
        return http.baseUrl(baseUrl())
                .acceptHeader("application/json")
                .acceptEncodingHeader("gzip, deflate")
                .acceptLanguageHeader(locale())
                .contentTypeHeader("application/x-www-form-urlencoded")
                .userAgentHeader("Gatling OAuth2 Performance Test");
    }
}
