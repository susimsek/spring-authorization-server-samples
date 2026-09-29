package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import gatling.GatlingDefaults;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class OAuth2Simulation extends Simulation {

    private final HttpProtocolBuilder httpProtocol = GatlingDefaults.httpProtocol();

    private final ChainBuilder discoveryFlow =
            exec(http("OpenID Configuration")
                            .get("/.well-known/openid-configuration")
                            .check(
                                    status().is(200),
                                    jsonPath("$.issuer").exists(),
                                    jsonPath("$.token_endpoint").exists()))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("JWK Set")
                                    .get("/oauth2/jwks")
                                    .check(
                                            status().is(200),
                                            jsonPath("$.keys[*].kid").exists(),
                                            jsonPath("$.keys[*].kty").is("RSA"),
                                            jsonPath("$.keys[*].use").is("sig"),
                                            jsonPath("$.keys[*].alg").is("RS256")))
                    .pause(GatlingDefaults.minPause(), GatlingDefaults.maxPause());

    private final ChainBuilder tokenLifecycleFlow =
            exec(http("Client Credentials Token")
                            .post("/oauth2/token")
                            .header("Authorization", GatlingDefaults.basicAuthorizationValue())
                            .header("Accept-Language", GatlingDefaults.locale())
                            .formParam("grant_type", "client_credentials")
                            .formParam("scope", GatlingDefaults.scope())
                            .check(
                                    status().is(200),
                                    jsonPath("$.access_token").exists().saveAs("access_token"),
                                    jsonPath("$.token_type").is("Bearer")))
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Token Introspection")
                                    .post("/oauth2/introspect")
                                    .header(
                                            "Authorization",
                                            GatlingDefaults.basicAuthorizationValue())
                                    .header("Accept-Language", GatlingDefaults.locale())
                                    .formParam("token", "#{access_token}")
                                    .check(status().is(200), jsonPath("$.active").is("true")))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Token Revocation")
                                    .post("/oauth2/revoke")
                                    .header(
                                            "Authorization",
                                            GatlingDefaults.basicAuthorizationValue())
                                    .header("Accept-Language", GatlingDefaults.locale())
                                    .formParam("token", "#{access_token}")
                                    .formParam("token_type_hint", "access_token")
                                    .check(status().is(200)))
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Revoked Token Introspection")
                                    .post("/oauth2/introspect")
                                    .header(
                                            "Authorization",
                                            GatlingDefaults.basicAuthorizationValue())
                                    .header("Accept-Language", GatlingDefaults.locale())
                                    .formParam("token", "#{access_token}")
                                    .check(status().is(200), jsonPath("$.active").is("false")))
                    .pause(GatlingDefaults.minPause(), GatlingDefaults.maxPause());

    private final ChainBuilder negativeProtocolFlow =
            exec(http("Invalid Client Credentials")
                            .post("/oauth2/token")
                            .header(
                                    "Authorization",
                                    GatlingDefaults.basicAuthorizationValue(
                                            GatlingDefaults.clientId(), "invalid-secret"))
                            .formParam("grant_type", "client_credentials")
                            .check(status().is(401), jsonPath("$.error").is("invalid_client")))
                    .exec(
                            http("Invalid Token Introspection")
                                    .post("/oauth2/introspect")
                                    .header(
                                            "Authorization",
                                            GatlingDefaults.basicAuthorizationValue())
                                    .formParam("token", "not-a-token")
                                    .check(status().is(200), jsonPath("$.active").is("false")))
                    .exec(
                            http("Invalid Refresh Token")
                                    .post("/oauth2/token")
                                    .header(
                                            "Authorization",
                                            GatlingDefaults.basicAuthorizationValue())
                                    .formParam("grant_type", "refresh_token")
                                    .formParam("refresh_token", "not-a-refresh-token")
                                    .check(status().is(400), jsonPath("$.error").exists()))
                    .exec(
                            http("OIDC Session Status")
                                    .get("/oidc/session-status")
                                    .check(status().is(401)));

    private final ChainBuilder authorizationCodePkceFlow =
            exec(http("PKCE Login Page")
                            .get("/login")
                            .header("Accept", "text/html")
                            .check(status().is(200)))
                    .exec(
                            http("PKCE Form Login")
                                    .post("/login")
                                    .header("Accept", "text/html")
                                    .formParam("username", GatlingDefaults.accountUsername())
                                    .formParam("password", GatlingDefaults.accountPassword())
                                    .check(
                                            status().is(302),
                                            header("Location")
                                                    .is(GatlingDefaults.baseUrl() + "/admin")))
                    .exitHereIfFailed()
                    .exec(
                            http("PKCE Authorization Request")
                                    .get("/oauth2/authorize")
                                    .header("Accept", "text/html")
                                    .queryParam("response_type", "code")
                                    .queryParam("client_id", "account-console")
                                    .queryParam("scope", "openid profile account-api")
                                    .queryParam(
                                            "redirect_uri",
                                            "http://localhost:9090/account/callback")
                                    .queryParam(
                                            "code_challenge", GatlingDefaults.pkceCodeChallenge())
                                    .queryParam("code_challenge_method", "S256")
                                    .queryParam("state", "gatling-pkce-state")
                                    .queryParam("nonce", "gatling-pkce-nonce")
                                    .check(
                                            status().is(302),
                                            header("Location").saveAs("pkce_redirect")))
                    .exitHereIfFailed()
                    .exec(
                            session ->
                                    session.set(
                                            "authorization_code",
                                            queryParameter(
                                                    session.getString("pkce_redirect"), "code")))
                    .exec(
                            http("PKCE Token Exchange")
                                    .post("/oauth2/token")
                                    .formParam("client_id", "account-console")
                                    .formParam("grant_type", "authorization_code")
                                    .formParam("code", "#{authorization_code}")
                                    .formParam(
                                            "redirect_uri",
                                            "http://localhost:9090/account/callback")
                                    .formParam("code_verifier", GatlingDefaults.pkceCodeVerifier())
                                    .check(
                                            status().is(200),
                                            jsonPath("$.access_token")
                                                    .exists()
                                                    .saveAs("account_access_token"),
                                            jsonPath("$.refresh_token")
                                                    .exists()
                                                    .saveAs("previous_refresh_token")))
                    .exitHereIfFailed()
                    .exec(
                            http("Account API Session Isolation")
                                    .get("/api/account/profile")
                                    .header("Authorization", "Bearer #{account_access_token}")
                                    .check(
                                            status().is(200),
                                            jsonPath("$.username")
                                                    .is(GatlingDefaults.accountUsername())))
                    .exec(
                            http("Refresh Token Rotation")
                                    .post("/oauth2/token")
                                    .formParam("client_id", "account-console")
                                    .formParam("grant_type", "refresh_token")
                                    .formParam("refresh_token", "#{previous_refresh_token}")
                                    .check(
                                            status().is(200),
                                            jsonPath("$.refresh_token")
                                                    .exists()
                                                    .saveAs("rotated_refresh_token")))
                    .exitHereIfFailed()
                    .exec(
                            http("Previous Refresh Token Rejected")
                                    .post("/oauth2/token")
                                    .formParam("client_id", "account-console")
                                    .formParam("grant_type", "refresh_token")
                                    .formParam("refresh_token", "#{previous_refresh_token}")
                                    .check(status().is(400), jsonPath("$.error").exists()));

    private final ScenarioBuilder oauth2LifecycleUsers =
            scenario("OAuth2 HTTP Lifecycle").exec(discoveryFlow).repeat(2).on(tokenLifecycleFlow);

    private final ScenarioBuilder negativeProtocolUsers =
            scenario("OAuth2 Negative Protocol Checks").exec(negativeProtocolFlow);

    private final ScenarioBuilder authorizationCodePkceUsers =
            scenario("Authorization Code PKCE and Refresh Rotation")
                    .exec(authorizationCodePkceFlow);

    {
        setUp(
                        oauth2LifecycleUsers.injectClosed(
                                rampConcurrentUsers(0)
                                        .to(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration()),
                                constantConcurrentUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.testDuration())),
                        negativeProtocolUsers.injectClosed(
                                constantConcurrentUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.testDuration())),
                        authorizationCodePkceUsers.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(httpProtocol)
                .assertions(
                        global().failedRequests()
                                .percent()
                                .lte(GatlingDefaults.maxFailurePercentage()),
                        global().responseTime()
                                .percentile3()
                                .lte(GatlingDefaults.maxResponseTimeMillis()),
                        global().responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("OpenID Configuration")
                                .responseTime()
                                .percentile3()
                                .lte(GatlingDefaults.maxResponseTimeMillis()),
                        details("JWK Set")
                                .responseTime()
                                .percentile3()
                                .lte(GatlingDefaults.maxResponseTimeMillis()),
                        details("Client Credentials Token")
                                .responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("Token Introspection")
                                .responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("Token Revocation")
                                .responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("OIDC Session Status")
                                .responseTime()
                                .percentile3()
                                .lte(GatlingDefaults.maxResponseTimeMillis()),
                        details("PKCE Token Exchange")
                                .responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("Refresh Token Rotation")
                                .responseTime()
                                .percentile4()
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()),
                        details("Account API Session Isolation")
                                .responseTime()
                                .percentile3()
                                .lte(GatlingDefaults.maxResponseTimeMillis()))
                .maxDuration(GatlingDefaults.maxDuration());
    }

    private static String queryParameter(String location, String name) {
        String query = URI.create(location).getRawQuery();
        for (String entry : query.split("&")) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("Missing " + name + " in authorization redirect");
    }
}
