package io.github.susimsek.springauthserversamples.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Cache cache = new Cache();

    private AuthorizationServer authorizationServer = new AuthorizationServer();

    @Getter
    @Setter
    public static class Cache {

        private Caffeine caffeine = new Caffeine();
    }

    @Getter
    @Setter
    public static class Caffeine {

        private Duration ttl = Duration.ofHours(1);

        private int initialCapacity = 500;

        private long maximumSize = 1000L;
    }

    @Getter
    @Setter
    public static class AuthorizationServer {

        private String issuer = "http://127.0.0.1:9090";

        private Jwk jwk = new Jwk();
    }

    @Getter
    @Setter
    public static class Jwk {

        private String publicKey;

        private String privateKey;

        private String keyId;
    }
}
