package io.github.susimsek.springauthserversamples.service.account;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.dto.account.AccountApplicationDTO;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AuthorizationConsentRepository authorizationConsentRepository;
    private final AuthorizationRepository authorizationRepository;
    private final ClientRepository clientRepository;
    private final AuthorizationServerMapperSupport mapperSupport;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public Page<AccountApplicationDTO> applications(String username, Pageable pageable) {
        Page<AuthorizationConsentEntity> consents =
                authorizationConsentRepository.findByIdPrincipalName(username, pageable);
        Map<String, String> clientNames =
                clientRepository
                        .findAllById(
                                consents.stream()
                                        .map(consent -> consent.getId().getRegisteredClientId())
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        client -> client.getId(),
                                        client -> client.getClientName()));
        return consents.map(
                consent -> {
                    String clientId = consent.getId().getRegisteredClientId();
                    Set<String> scopes =
                            mapperSupport.readAuthorities(consent.getAuthorities()).stream()
                                    .map(authority -> authority.getAuthority())
                                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
                    return new AccountApplicationDTO(
                            clientId,
                            clientNames.getOrDefault(clientId, clientId),
                            scopes,
                            consent.getCreatedAt(),
                            consent.getUpdatedAt());
                });
    }

    @Transactional
    public void revokeApplication(String username, String clientId) {
        AuthorizationConsentId id = new AuthorizationConsentId(clientId, username);
        if (!authorizationConsentRepository.existsById(id)) {
            throw ApiException.notFound("Application consent not found");
        }
        authorizationConsentRepository.deleteById(id);
        authorizationRepository.deleteByPrincipalNameAndRegisteredClientId(username, clientId);
        auditEventService.record(
                "account.application.revoked", "consent", clientId + ":" + username);
    }
}
