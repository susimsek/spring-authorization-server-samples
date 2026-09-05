package io.github.susimsek.springauthserversamples.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
        @DefaultValue Cache cache,
        @DefaultValue Session session,
        @DefaultValue AuthorizationServer authorizationServer,
        @DefaultValue Mail mail,
        @DefaultValue Security security) {

    public ApplicationProperties() {
        this(
                new Cache(new Caffeine(Duration.ofHours(1), 500, 1000)),
                new Session("0 * * * * *"),
                new AuthorizationServer("http://127.0.0.1:9090"),
                new Mail(
                        false,
                        "Spring Authorization Server <no-reply@localhost>",
                        "http://127.0.0.1:9090"),
                new Security());
    }

    public ApplicationProperties(
            Cache cache, Session session, AuthorizationServer authorizationServer, Mail mail) {
        this(cache, session, authorizationServer, mail, new Security());
    }

    public record Cache(@DefaultValue Caffeine caffeine) {}

    public record Caffeine(
            @DefaultValue("PT1H") Duration ttl,
            @DefaultValue("500") int initialCapacity,
            @DefaultValue("1000") long maximumSize) {}

    public record Session(@DefaultValue("0 * * * * *") String cleanupCron) {}

    public record AuthorizationServer(@DefaultValue("http://127.0.0.1:9090") String issuer) {}

    public record Mail(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("Spring Authorization Server <no-reply@localhost>") String from,
            @DefaultValue("http://127.0.0.1:9090") String baseUrl) {}

    public record Security(
            @DefaultValue PasswordPolicy passwordPolicy, @DefaultValue BruteForce bruteForce) {

        public Security() {
            this(new PasswordPolicy(), new BruteForce());
        }
    }

    public record PasswordPolicy(
            @DefaultValue("12") int minimumLength,
            @DefaultValue("128") int maximumLength,
            @DefaultValue("1") int minimumUppercase,
            @DefaultValue("1") int minimumLowercase,
            @DefaultValue("1") int minimumDigits,
            @DefaultValue("1") int minimumSpecialCharacters,
            @DefaultValue("true") boolean rejectUsername,
            @DefaultValue("true") boolean rejectEmail,
            @DefaultValue("true") boolean rejectCommonPasswords,
            @DefaultValue("5") int historySize,
            @DefaultValue("90") int expirationDays,
            @DefaultValue("password,123456,12345678,qwerty,qwerty123,admin,letmein")
                    String commonPasswords) {

        public PasswordPolicy() {
            this(
                    12,
                    128,
                    1,
                    1,
                    1,
                    1,
                    true,
                    true,
                    true,
                    5,
                    90,
                    "password,123456,12345678,qwerty,qwerty123,admin,letmein");
        }
    }

    public record BruteForce(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("5") int maxFailures,
            @DefaultValue("PT1S") Duration quickLoginWindow,
            @DefaultValue("PT1M") Duration minimumQuickLoginWait,
            @DefaultValue("PT1M") Duration waitIncrement,
            @DefaultValue("PT15M") Duration maxWait,
            @DefaultValue("PT12H") Duration failureResetTime,
            @DefaultValue("3") int maxTemporaryLockouts,
            @DefaultValue("false") boolean permanentLockout,
            @DefaultValue("30") int ipRequestsPerMinute,
            @DefaultValue("5") int usernameIpRequestsPerMinute) {

        public BruteForce() {
            this(
                    true,
                    5,
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(15),
                    Duration.ofHours(12),
                    3,
                    false,
                    30,
                    5);
        }
    }
}
