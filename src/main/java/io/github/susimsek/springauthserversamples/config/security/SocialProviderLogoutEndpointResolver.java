package io.github.susimsek.springauthserversamples.config.security;

import io.github.susimsek.springauthserversamples.service.SocialProviderSettingsService.ProviderCredentials;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Resolves the browser logout endpoint exposed by the linked identity provider. */
@Component
public class SocialProviderLogoutEndpointResolver {

    private static final String OPENID_CONFIGURATION = "/.well-known/openid-configuration";
    private final RestClient restClient;
    private final ConcurrentMap<String, String> discoveredEndpoints = new ConcurrentHashMap<>();

    @Autowired
    public SocialProviderLogoutEndpointResolver() {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    SocialProviderLogoutEndpointResolver(RestClient restClient) {
        this.restClient = restClient;
    }

    public String resolve(ProviderCredentials provider) {
        if (provider == null) {
            return null;
        }
        String issuer = trimToNull(provider.issuerUri());
        if (issuer != null) {
            String discovered = discover(issuer);
            if (discovered != null) {
                return discovered;
            }
        }
        return switch (provider.providerType().toLowerCase(java.util.Locale.ROOT)) {
            case "google" -> "https://accounts.google.com/Logout";
            case "microsoft" -> "https://login.microsoftonline.com/common/oauth2/v2.0/logout";
            case "github" -> "https://github.com/logout";
            case "linkedin" -> "https://www.linkedin.com/m/logout";
            default -> null;
        };
    }

    private String discover(String issuer) {
        String cached = discoveredEndpoints.get(issuer);
        if (cached != null) {
            return cached;
        }
        String discovered = fetchDiscovery(issuer);
        if (discovered != null) {
            discoveredEndpoints.putIfAbsent(issuer, discovered);
        }
        return discovered;
    }

    @SuppressWarnings("unchecked")
    private String fetchDiscovery(String issuer) {
        try {
            URI issuerUri = URI.create(issuer);
            if (!isHttp(issuerUri)) {
                return null;
            }
            String metadataUri =
                    issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
            metadataUri += OPENID_CONFIGURATION;
            Map<String, Object> metadata =
                    restClient.get().uri(metadataUri).retrieve().body(Map.class);
            if (metadata == null) {
                return null;
            }
            Object endpoint = metadata.get("end_session_endpoint");
            if (!(endpoint instanceof String value) || !isHttp(URI.create(value))) {
                return null;
            }
            return value;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isHttp(URI uri) {
        return uri != null
                && ("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
