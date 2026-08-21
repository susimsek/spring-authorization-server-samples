package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminConsentService {

    private final AdminUserService adminUserService;
    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final AuthorizationRepository authorizationRepository;
    private final ClientRepository clientRepository;
    private final AuthorizationServerMapperSupport mapperSupport;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<ConsentView> consents(String query, Pageable pageable) {
        Page<AuthorizationConsentEntity> consents =
                authorizationConsentRepository.search(AdminSearch.normalize(query), pageable);
        Map<String, String> clientNames =
                clientRepository
                        .findAllById(
                                consents.getContent().stream()
                                        .map(consent -> consent.getId().getRegisteredClientId())
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        client -> client.getId(),
                                        client -> client.getClientName()));
        return consents.map(consent -> consentView(consent, clientNames));
    }

    @Transactional
    public void revokeConsent(String clientId, String username, String currentUsername) {
        adminUserService.assertCanManageUsername(username, currentUsername);
        AuthorizationConsentId id = new AuthorizationConsentId(clientId, username);
        if (!authorizationConsentRepository.existsById(id)) {
            throw AdminClientException.notFound("Consent not found");
        }
        authorizationConsentRepository.deleteById(id);
        authorizationRepository.deleteByPrincipalNameAndRegisteredClientId(username, clientId);
        adminAuditEventService.record("consent.revoked", "consent", clientId + ":" + username);
    }

    private ConsentView consentView(
            AuthorizationConsentEntity consent, Map<String, String> clientNames) {
        String clientId = consent.getId().getRegisteredClientId();
        return new ConsentView(
                clientId,
                clientNames.getOrDefault(clientId, clientId),
                consent.getId().getPrincipalName(),
                mapperSupport.readAuthorities(consent.getAuthorities()).stream()
                        .map(authority -> authority.getAuthority())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    public record ConsentView(
            String clientId, String clientName, String principalName, Set<String> authorities) {}
}
