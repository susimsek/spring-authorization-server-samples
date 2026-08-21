package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.mapper.RegisteredClientMapper;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.web.admin.AdminClientCreatedView;
import io.github.susimsek.springauthserversamples.web.admin.AdminClientRequest;
import io.github.susimsek.springauthserversamples.web.admin.AdminClientView;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminClientService {

    private static final String ADMIN_CONSOLE_CLIENT_ID = "admin-console";
    private static final Duration DEFAULT_AUTHORIZATION_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofHours(1);

    private final ClientRepository clientRepository;
    private final AuthorizationRepository authorizationRepository;
    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final RegisteredClientMapper registeredClientMapper;
    private final AuthorizationServerMapperSupport mapperSupport;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<AdminClientView> findAll(String query, Pageable pageable) {
        String searchQuery = AdminSearch.normalize(query);
        return clientRepository
                .findByClientIdContainingIgnoreCaseOrClientNameContainingIgnoreCase(
                        searchQuery, searchQuery, pageable)
                .map(entity -> registeredClientMapper.toObject(entity, mapperSupport))
                .map(AdminClientService::toView);
    }

    @Transactional(readOnly = true)
    public AdminClientView findById(String id) {
        return clientRepository
                .findById(id)
                .map(entity -> registeredClientMapper.toObject(entity, mapperSupport))
                .map(AdminClientService::toView)
                .orElse(null);
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public AdminClientCreatedView create(AdminClientRequest request) {
        validate(request);
        if (clientRepository.existsByClientId(request.clientId())) {
            throw AdminClientException.conflict(
                    "clientId",
                    "admin_client_duplicate_client_id",
                    "Client ID is already registered");
        }

        String rawSecret = requiresSecret(request) ? generateSecret() : null;
        RegisteredClient client =
                apply(
                                RegisteredClient.withId(UUID.randomUUID().toString())
                                        .clientId(request.clientId())
                                        .clientIdIssuedAt(Instant.now())
                                        .clientSecret(
                                                rawSecret == null
                                                        ? null
                                                        : passwordEncoder.encode(rawSecret)),
                                request,
                                null)
                        .build();

        AdminClientView saved = toView(save(client));
        adminAuditEventService.record("client.created", "client", saved.id());
        return new AdminClientCreatedView(saved, rawSecret);
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public AdminClientView update(String id, AdminClientRequest request) {
        validate(request);
        RegisteredClient existing = findRequired(id);
        rejectAdminConsoleMutation(existing);
        if (!existing.getClientId().equals(request.clientId())
                && clientRepository.existsByClientId(request.clientId())) {
            throw AdminClientException.conflict(
                    "clientId",
                    "admin_client_duplicate_client_id",
                    "Client ID is already registered");
        }

        RegisteredClient.Builder builder =
                RegisteredClient.from(existing)
                        .clientId(request.clientId())
                        .clientName(request.clientName());

        if (!requiresSecret(request)) {
            builder.clientSecret(null).clientSecretExpiresAt(null);
        } else if (existing.getClientSecret() == null) {
            throw AdminClientException.badRequest(
                    "clientAuthenticationMethods",
                    "admin_client_secret_required",
                    "Regenerate a client secret before enabling a secret authentication method");
        }

        RegisteredClient updated = apply(builder, request, existing).build();
        AdminClientView saved = toView(save(updated));
        adminAuditEventService.record("client.updated", "client", saved.id());
        return saved;
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public void delete(String id) {
        rejectAdminConsoleMutation(findRequired(id));
        authorizationRepository.deleteByRegisteredClientId(id);
        authorizationConsentRepository.deleteByIdRegisteredClientId(id);
        clientRepository.deleteById(id);
        adminAuditEventService.record("client.deleted", "client", id);
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public String regenerateSecret(String id) {
        RegisteredClient existing = findRequired(id);
        rejectAdminConsoleMutation(existing);

        String rawSecret = generateSecret();
        RegisteredClient updated =
                RegisteredClient.from(existing)
                        .clientSecret(passwordEncoder.encode(rawSecret))
                        .build();
        save(updated);
        adminAuditEventService.record("client.secret.regenerated", "client", id);
        return rawSecret;
    }

    private static void validate(AdminClientRequest request) {
        if (request == null) {
            throw AdminClientException.badRequest(
                    "admin_client_invalid_request", "Request body is required");
        }
        if (!hasText(request.clientId())) {
            throw AdminClientException.badRequest(
                    "clientId", "admin_client_invalid_client_id", "Client ID is required");
        }
        if (!hasText(request.clientName())) {
            throw AdminClientException.badRequest(
                    "clientName", "admin_client_invalid_client_name", "Client name is required");
        }
        requireNonEmpty(
                request.clientAuthenticationMethods(),
                "clientAuthenticationMethods",
                "admin_client_invalid_authentication_methods",
                "At least one client authentication method is required");
        requireNonEmpty(
                request.authorizationGrantTypes(),
                "authorizationGrantTypes",
                "admin_client_invalid_grant_types",
                "At least one authorization grant type is required");
        requireNonEmpty(
                request.scopes(),
                "scopes",
                "admin_client_invalid_scopes",
                "At least one scope is required");

        Set<String> methods = request.clientAuthenticationMethods();
        Set<String> grants = request.authorizationGrantTypes();
        Set<String> redirectUris = nullSafe(request.redirectUris());

        boolean publicClient = methods.contains(ClientAuthenticationMethod.NONE.getValue());
        if (publicClient && methods.size() > 1) {
            throw AdminClientException.badRequest(
                    "clientAuthenticationMethods",
                    "admin_client_invalid_authentication_methods",
                    "The 'none' authentication method cannot be combined with other methods");
        }
        if (publicClient && grants.contains(AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())) {
            throw AdminClientException.badRequest(
                    "authorizationGrantTypes",
                    "admin_client_invalid_grant_types",
                    "A public client cannot use the client_credentials grant");
        }
        if (grants.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                && redirectUris.isEmpty()) {
            throw AdminClientException.badRequest(
                    "redirectUris",
                    "admin_client_redirect_uri_required",
                    "At least one redirect URI is required for authorization_code");
        }
        if (publicClient
                && grants.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())
                && !request.requireProofKey()) {
            throw AdminClientException.badRequest(
                    "authorizationGrantTypes",
                    "admin_client_pkce_required",
                    "PKCE must be required for a public authorization_code client");
        }
        if (request.requireProofKey()
                && !grants.contains(AuthorizationGrantType.AUTHORIZATION_CODE.getValue())) {
            throw AdminClientException.badRequest(
                    "authorizationGrantTypes",
                    "admin_client_invalid_pkce",
                    "PKCE requires the authorization_code grant");
        }

        redirectUris.forEach(uri -> validateUri("redirectUris", "redirect URI", uri));
        nullSafe(request.postLogoutRedirectUris())
                .forEach(
                        uri ->
                                validateUri(
                                        "postLogoutRedirectUris", "post logout redirect URI", uri));

        validatePositiveDuration(
                "authorizationCodeTimeToLive",
                "authorization code TTL",
                request.authorizationCodeTimeToLive());
        validatePositiveDuration(
                "accessTokenTimeToLive", "access token TTL", request.accessTokenTimeToLive());
        validatePositiveDuration(
                "refreshTokenTimeToLive", "refresh token TTL", request.refreshTokenTimeToLive());
    }

    private RegisteredClient save(RegisteredClient client) {
        return registeredClientMapper.toObject(
                clientRepository.save(registeredClientMapper.toEntity(client, mapperSupport)),
                mapperSupport);
    }

    private RegisteredClient findRequired(String id) {
        return clientRepository
                .findById(id)
                .map(entity -> registeredClientMapper.toObject(entity, mapperSupport))
                .orElseThrow(() -> AdminClientException.notFound("Client not found"));
    }

    private static void rejectAdminConsoleMutation(RegisteredClient client) {
        if (ADMIN_CONSOLE_CLIENT_ID.equals(client.getClientId())) {
            throw AdminClientException.badRequest(
                    "admin_client_protected",
                    "The administration console client cannot be changed");
        }
    }

    private static RegisteredClient.Builder apply(
            RegisteredClient.Builder builder,
            AdminClientRequest request,
            RegisteredClient existing) {
        builder.clientName(request.clientName())
                .clientAuthenticationMethods(
                        methods -> {
                            methods.clear();
                            request.clientAuthenticationMethods().stream()
                                    .map(ClientAuthenticationMethod::new)
                                    .forEach(methods::add);
                        })
                .authorizationGrantTypes(
                        grantTypes -> {
                            grantTypes.clear();
                            request.authorizationGrantTypes().stream()
                                    .map(AuthorizationGrantType::new)
                                    .forEach(grantTypes::add);
                        })
                .redirectUris(
                        uris -> {
                            uris.clear();
                            uris.addAll(nullSafe(request.redirectUris()));
                        })
                .postLogoutRedirectUris(
                        uris -> {
                            uris.clear();
                            uris.addAll(nullSafe(request.postLogoutRedirectUris()));
                        })
                .scopes(
                        scopes -> {
                            scopes.clear();
                            scopes.addAll(request.scopes());
                        })
                .clientSettings(buildClientSettings(request, existing))
                .tokenSettings(buildTokenSettings(request, existing));
        return builder;
    }

    private static ClientSettings buildClientSettings(
            AdminClientRequest request, RegisteredClient existing) {
        ClientSettings.Builder builder =
                existing == null
                        ? ClientSettings.builder()
                        : ClientSettings.withSettings(
                                new HashMap<>(existing.getClientSettings().getSettings()));

        return builder.requireAuthorizationConsent(request.requireAuthorizationConsent())
                .requireProofKey(request.requireProofKey())
                .build();
    }

    private static TokenSettings buildTokenSettings(
            AdminClientRequest request, RegisteredClient existing) {
        TokenSettings.Builder builder =
                existing == null
                        ? TokenSettings.builder()
                        : TokenSettings.withSettings(
                                new HashMap<>(existing.getTokenSettings().getSettings()));

        Duration authorizationCodeTtl =
                request.authorizationCodeTimeToLive() != null
                        ? request.authorizationCodeTimeToLive()
                        : existing == null
                                ? DEFAULT_AUTHORIZATION_CODE_TTL
                                : existing.getTokenSettings().getAuthorizationCodeTimeToLive();
        Duration accessTokenTtl =
                request.accessTokenTimeToLive() != null
                        ? request.accessTokenTimeToLive()
                        : existing == null
                                ? DEFAULT_ACCESS_TOKEN_TTL
                                : existing.getTokenSettings().getAccessTokenTimeToLive();
        Duration refreshTokenTtl =
                request.refreshTokenTimeToLive() != null
                        ? request.refreshTokenTimeToLive()
                        : existing == null
                                ? DEFAULT_REFRESH_TOKEN_TTL
                                : existing.getTokenSettings().getRefreshTokenTimeToLive();

        return builder.authorizationCodeTimeToLive(authorizationCodeTtl)
                .accessTokenTimeToLive(accessTokenTtl)
                .refreshTokenTimeToLive(refreshTokenTtl)
                .build();
    }

    private static boolean requiresSecret(AdminClientRequest request) {
        return request.clientAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::new)
                .anyMatch(AdminClientService::isSecretMethod);
    }

    private static boolean isSecretMethod(ClientAuthenticationMethod method) {
        return ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(method)
                || ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(method);
    }

    private static void validateUri(String field, String label, String value) {
        if (!hasText(value)) {
            throw AdminClientException.badRequest(
                    field, "admin_client_invalid_uri", "Empty " + label + " is not allowed");
        }
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getScheme() == null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw AdminClientException.badRequest(
                    field, "admin_client_invalid_uri", "Invalid " + label + ": " + value);
        }
    }

    private static void validatePositiveDuration(String field, String label, Duration duration) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw AdminClientException.badRequest(
                    field, "admin_client_invalid_ttl", label + " must be greater than zero");
        }
    }

    private static <T> void requireNonEmpty(
            Set<T> values, String field, String errorCode, String message) {
        if (values == null || values.isEmpty()) {
            throw AdminClientException.badRequest(field, errorCode, message);
        }
    }

    private static Set<String> nullSafe(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String generateSecret() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private static AdminClientView toView(RegisteredClient client) {
        return new AdminClientView(
                client.getId(),
                client.getClientId(),
                client.getClientName(),
                client.getClientIdIssuedAt(),
                client.getClientSecretExpiresAt(),
                client.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                client.getRedirectUris(),
                client.getPostLogoutRedirectUris(),
                client.getScopes(),
                client.getClientSettings().isRequireAuthorizationConsent(),
                client.getClientSettings().isRequireProofKey(),
                client.getTokenSettings().getAuthorizationCodeTimeToLive(),
                client.getTokenSettings().getAccessTokenTimeToLive(),
                client.getTokenSettings().getRefreshTokenTimeToLive());
    }
}
