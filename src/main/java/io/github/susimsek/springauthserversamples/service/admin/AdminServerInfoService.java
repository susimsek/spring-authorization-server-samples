package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.OAuth2KeyEntity;
import io.github.susimsek.springauthserversamples.repository.OAuth2KeyRepository;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServerInfoService {

    private final ApplicationProperties applicationProperties;
    private final SessionProperties sessionProperties;
    private final OAuth2KeyRepository oauth2KeyRepository;

    @Transactional(readOnly = true)
    public ServerInfoView serverInfo() {
        String issuer = applicationProperties.authorizationServer().issuer();
        Duration sessionTimeout = sessionProperties.getTimeout();

        Optional<OAuth2KeyEntity> activeKey =
                oauth2KeyRepository.findAllKeys().stream()
                        .filter(OAuth2KeyEntity::isActive)
                        .max(Comparator.comparing(OAuth2KeyEntity::getCreatedAt));

        return new ServerInfoView(
                issuer,
                issuer + "/.well-known/openid-configuration",
                issuer + "/oauth2/authorize",
                issuer + "/oauth2/token",
                issuer + "/oauth2/introspect",
                issuer + "/oauth2/revoke",
                issuer + "/oauth2/jwks",
                issuer + "/userinfo",
                issuer + "/connect/logout",
                sessionTimeout == null ? null : sessionTimeout.toString(),
                activeKey.map(KeySummary::from).orElse(null));
    }

    public record ServerInfoView(
            String issuer,
            String discoveryEndpoint,
            String authorizationEndpoint,
            String tokenEndpoint,
            String introspectionEndpoint,
            String revocationEndpoint,
            String jwksEndpoint,
            String userInfoEndpoint,
            String endSessionEndpoint,
            String sessionTimeout,
            KeySummary activeSigningKey) {}

    public record KeySummary(
            String kid, String type, String algorithm, String use, java.time.Instant createdAt) {
        static KeySummary from(OAuth2KeyEntity key) {
            return new KeySummary(
                    key.getKid(),
                    key.getType(),
                    key.getAlgorithm(),
                    key.getUse(),
                    key.getCreatedAt());
        }
    }
}
