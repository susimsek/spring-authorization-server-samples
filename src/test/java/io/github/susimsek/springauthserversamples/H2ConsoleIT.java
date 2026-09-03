package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.test.context.ActiveProfiles;

// H2 is a separate servlet; a real HTTP server is required instead of MockMvc.
@SpringBootTest(
        classes = SpringAuthorizationServerSamplesApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:h2-console-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.show-sql=false",
            "logging.level.org.hibernate.SQL=WARN",
            "logging.level.io.github.susimsek.springauthserversamples=INFO"
        })
@ActiveProfiles("dev")
class H2ConsoleIT {

    @LocalServerPort private int port;

    @Autowired
    @Qualifier("h2Console")
    private ServletRegistrationBean<?> h2Console;

    @Test
    void servesLocalConsoleWithFramesWithoutWeakeningApplicationSecurity() throws Exception {
        assertThat(h2Console.getUrlMappings()).containsExactly("/h2-console/*");
        assertThat(h2Console.getInitParameters()).doesNotContainKey("webAllowOthers");
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpResponse<String> console = get(client, "/h2-console/", "text/html");
            assertThat(console.statusCode()).isEqualTo(200);
            assertThat(console.body()).contains("H2 Console").doesNotContain("/_next/");
            assertThat(console.headers().firstValue("X-Frame-Options")).contains("SAMEORIGIN");

            var session = Pattern.compile("jsessionid=([a-f0-9]+)").matcher(console.body());
            assertThat(session.find()).isTrue();
            String sessionQuery = "?jsessionid=" + session.group(1);
            String form =
                    "driver=org.h2.Driver&user=sa&password=&url="
                            + URLEncoder.encode(
                                    "jdbc:h2:mem:h2-console-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                                    StandardCharsets.UTF_8);
            HttpResponse<String> connected =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "http://localhost:"
                                                            + port
                                                            + "/h2-console/login.do"
                                                            + sessionQuery))
                                    .timeout(Duration.ofSeconds(10))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(HttpRequest.BodyPublishers.ofString(form))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            assertThat(connected.statusCode()).isEqualTo(200);
            assertThat(connected.body()).contains("frame", "query.jsp");
            assertThat(get(client, "/h2-console/tables.do" + sessionQuery, "text/html").body())
                    .contains("OAUTH2_REGISTERED_CLIENT", "USER_SESSION");
            get(client, "/h2-console/logout.do" + sessionQuery, "text/html");

            HttpResponse<String> login = get(client, "/login", "text/html");
            assertThat(login.statusCode()).isEqualTo(200);
            assertThat(login.headers().firstValue("X-Frame-Options")).contains("DENY");
            assertThat(get(client, "/api/admin/users", "application/json").statusCode())
                    .isEqualTo(401);
            assertThat(get(client, "/api/account/profile", "application/json").statusCode())
                    .isEqualTo(401);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path, String accept)
            throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", accept)
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
