package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.ClientScopeEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.mapper.RegisteredClientMapper;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.ClientScopeRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.web.admin.AdminClientScopeAssignmentRequest;
import io.github.susimsek.springauthserversamples.web.admin.AdminClientScopeRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminClientScopeService {

    private final ClientScopeRepository clientScopeRepository;
    private final ClientRepository clientRepository;
    private final RegisteredClientMapper registeredClientMapper;
    private final AuthorizationServerMapperSupport mapperSupport;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<ClientScopeView> findAll(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        return clientScopeRepository
                .findByNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q, q, pageable)
                .map(ClientScopeView::from);
    }

    @Transactional(readOnly = true)
    public List<ClientScopeView> findAll() {
        return clientScopeRepository
                .findAll(org.springframework.data.domain.Sort.by("name"))
                .stream()
                .map(ClientScopeView::from)
                .toList();
    }

    @Transactional
    public ClientScopeView create(AdminClientScopeRequest request) {
        String name = normalizeName(request.name());
        if (clientScopeRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "admin_client_scope_duplicate", "Client scope already exists");
        }
        ClientScopeEntity entity = new ClientScopeEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setDisplayName(trimToNull(request.displayName()));
        entity.setDescription(trimToNull(request.description()));
        ClientScopeEntity saved = clientScopeRepository.save(entity);
        adminAuditEventService.record("client-scope.created", "client-scope", saved.getId());
        return ClientScopeView.from(saved);
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public ClientScopeView update(String id, AdminClientScopeRequest request) {
        ClientScopeEntity entity = required(id);
        String name = normalizeName(request.name());
        clientScopeRepository
                .findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(
                        ignored -> {
                            throw ApiException.conflict(
                                    "admin_client_scope_duplicate", "Client scope already exists");
                        });
        if (!entity.getName().equals(name)) {
            renameAssignedScope(entity.getName(), name);
        }
        entity.setName(name);
        entity.setDisplayName(trimToNull(request.displayName()));
        entity.setDescription(trimToNull(request.description()));
        ClientScopeEntity saved = clientScopeRepository.save(entity);
        adminAuditEventService.record("client-scope.updated", "client-scope", id);
        return ClientScopeView.from(saved);
    }

    @Transactional
    public void delete(String id) {
        ClientScopeEntity entity = required(id);
        boolean assigned =
                clientRepository.findAll().stream()
                        .anyMatch(
                                client ->
                                        mapperSupport
                                                .readCollection(client.getScopes())
                                                .contains(entity.getName()));
        if (assigned) {
            throw ApiException.badRequest(
                    "admin_client_scope_assigned", "Assigned client scopes cannot be deleted");
        }
        clientScopeRepository.delete(entity);
        adminAuditEventService.record("client-scope.deleted", "client-scope", id);
    }

    @Transactional(readOnly = true)
    public ScopeAssignments assignments(String clientId) {
        RegisteredClient client = clientRequired(clientId);
        Set<String> defaults = ClientScopeSettings.defaultScopes(client);
        Set<String> optional = ClientScopeSettings.optionalScopes(client);
        List<ClientScopeView> scopes = findAll();
        return new ScopeAssignments(defaults, optional, scopes);
    }

    @Transactional
    @CacheEvict(
            cacheNames = ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE,
            allEntries = true)
    public ScopeAssignments updateAssignments(
            String clientId, AdminClientScopeAssignmentRequest request) {
        Set<String> defaults = normalized(request.defaultScopes());
        Set<String> optional = normalized(request.optionalScopes());
        Set<String> intersection = new LinkedHashSet<>(defaults);
        intersection.retainAll(optional);
        if (!intersection.isEmpty()) {
            throw ApiException.badRequest(
                    "admin_client_scope_assignment_overlap",
                    "A scope cannot be both default and optional");
        }
        Set<String> union = new LinkedHashSet<>(defaults);
        union.addAll(optional);
        if (union.isEmpty()) {
            throw ApiException.badRequest(
                    "admin_client_invalid_scopes", "At least one client scope is required");
        }
        long known = clientScopeRepository.countByNameIn(union);
        if (known != union.size()) {
            throw ApiException.badRequest(
                    "admin_client_scope_unknown", "One or more client scopes do not exist");
        }

        RegisteredClient existing = clientRequired(clientId);
        RegisteredClient.Builder builder = RegisteredClient.from(existing);
        builder.scopes(
                scopes -> {
                    scopes.clear();
                    scopes.addAll(union);
                });
        builder.clientSettings(
                ClientScopeSettings.withAssignments(
                        existing.getClientSettings(), defaults, optional));
        RegisteredClient updated = builder.build();
        clientRepository.save(registeredClientMapper.toEntity(updated, mapperSupport));
        adminAuditEventService.record("client.scopes.updated", "client", clientId);
        return new ScopeAssignments(defaults, optional, findAll());
    }

    private void renameAssignedScope(String oldName, String newName) {
        clientRepository
                .findAll()
                .forEach(
                        entity -> {
                            RegisteredClient client =
                                    registeredClientMapper.toObject(entity, mapperSupport);
                            if (!client.getScopes().contains(oldName)) {
                                return;
                            }
                            Set<String> defaults =
                                    replace(
                                            ClientScopeSettings.defaultScopes(client),
                                            oldName,
                                            newName);
                            Set<String> optional =
                                    replace(
                                            ClientScopeSettings.optionalScopes(client),
                                            oldName,
                                            newName);
                            Set<String> scopes =
                                    replace(
                                            new LinkedHashSet<>(client.getScopes()),
                                            oldName,
                                            newName);
                            RegisteredClient updated =
                                    RegisteredClient.from(client)
                                            .scopes(
                                                    values -> {
                                                        values.clear();
                                                        values.addAll(scopes);
                                                    })
                                            .clientSettings(
                                                    ClientScopeSettings.withAssignments(
                                                            client.getClientSettings(),
                                                            defaults,
                                                            optional))
                                            .build();
                            clientRepository.save(
                                    registeredClientMapper.toEntity(updated, mapperSupport));
                        });
    }

    private RegisteredClient clientRequired(String id) {
        return clientRepository
                .findById(id)
                .map(entity -> registeredClientMapper.toObject(entity, mapperSupport))
                .orElseThrow(() -> ApiException.notFound("Client not found"));
    }

    private ClientScopeEntity required(String id) {
        return clientScopeRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("Client scope not found"));
    }

    private static Set<String> normalized(Set<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream().map(AdminClientScopeService::normalizeName).forEach(result::add);
        }
        return result;
    }

    private static Set<String> replace(Set<String> values, String oldName, String newName) {
        Set<String> result = new LinkedHashSet<>();
        values.forEach(value -> result.add(value.equals(oldName) ? newName : value));
        return result;
    }

    private static String normalizeName(String value) {
        String name = value == null ? "" : value.trim();
        if (!StringUtils.hasText(name)
                || name.length() > 100
                || name.chars().anyMatch(Character::isWhitespace)) {
            throw ApiException.badRequest(
                    "name", "admin_client_scope_invalid_name", "Client scope name is invalid");
        }
        return name;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record ClientScopeView(
            String id,
            String name,
            String displayName,
            String description,
            Instant createdAt,
            Instant updatedAt) {
        static ClientScopeView from(ClientScopeEntity entity) {
            return new ClientScopeView(
                    entity.getId(),
                    entity.getName(),
                    entity.getDisplayName(),
                    entity.getDescription(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt());
        }
    }

    public record ScopeAssignments(
            Set<String> defaultScopes,
            Set<String> optionalScopes,
            List<ClientScopeView> availableScopes) {}
}
