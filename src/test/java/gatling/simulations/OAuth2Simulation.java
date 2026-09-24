package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import gatling.GatlingDefaults;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

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
                                    .check(status().is(200), jsonPath("$.keys[*].kid").exists()))
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

    private final ScenarioBuilder oauth2LifecycleUsers =
            scenario("OAuth2 HTTP Lifecycle").exec(discoveryFlow).repeat(2).on(tokenLifecycleFlow);

    {
        setUp(
                        oauth2LifecycleUsers.injectClosed(
                                rampConcurrentUsers(0)
                                        .to(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration()),
                                constantConcurrentUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.testDuration())))
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
                                .lte(GatlingDefaults.maxP99ResponseTimeMillis()))
                .maxDuration(GatlingDefaults.maxDuration());
    }
}
