package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
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
                    .pause(GatlingDefaults.minPause(), GatlingDefaults.maxPause());

    private final ScenarioBuilder users =
            scenario("OAuth2 HTTP Lifecycle").exec(discoveryFlow).repeat(2).on(tokenLifecycleFlow);

    {
        setUp(
                        users.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(httpProtocol)
                .maxDuration(GatlingDefaults.maxDuration());
    }
}
