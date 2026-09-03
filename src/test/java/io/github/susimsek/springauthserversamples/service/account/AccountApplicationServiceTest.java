package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    @Mock private AuthorizationConsentRepository consentRepository;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private AuthorizationServerMapperSupport mapperSupport;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void mapsAuthorizedApplicationsInBatches() {
        final Pageable pageable = Pageable.unpaged();
        AuthorizationConsentEntity consent = new AuthorizationConsentEntity();
        consent.setId(new AuthorizationConsentId("client-id", "alice"));
        consent.setAuthorities("serialized");
        RegisteredClientEntity client = new RegisteredClientEntity();
        client.setId("client-id");
        client.setClientName("Client One");
        when(consentRepository.findByIdPrincipalName("alice", pageable))
                .thenReturn(new PageImpl<>(List.of(consent)));
        when(clientRepository.findAllById(List.of("client-id"))).thenReturn(List.of(client));
        when(mapperSupport.readAuthorities("serialized"))
                .thenReturn(java.util.Set.of(new SimpleGrantedAuthority("openid")));

        var result = service().applications("alice", pageable).getContent();

        assertThat(result)
                .singleElement()
                .satisfies(
                        application -> {
                            assertThat(application.clientId()).isEqualTo("client-id");
                            assertThat(application.clientName()).isEqualTo("Client One");
                            assertThat(application.scopes()).containsExactly("openid");
                        });
    }

    @Test
    void revokesConsentAndClientAuthorizations() {
        AuthorizationConsentId id = new AuthorizationConsentId("client-id", "alice");
        when(consentRepository.existsById(id)).thenReturn(true);

        service().revokeApplication("alice", "client-id");

        verify(consentRepository).deleteById(id);
        verify(authorizationRepository)
                .deleteByPrincipalNameAndRegisteredClientId("alice", "client-id");
        verify(auditEventService)
                .record("account.application.revoked", "consent", "client-id:alice");
    }

    @Test
    void rejectsUnknownConsent() {
        assertThatThrownBy(() -> service().revokeApplication("alice", "missing"))
                .isInstanceOf(ApiException.class);
    }

    private AccountApplicationService service() {
        return new AccountApplicationService(
                consentRepository,
                authorizationRepository,
                clientRepository,
                mapperSupport,
                auditEventService);
    }
}
