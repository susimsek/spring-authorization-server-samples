package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentId;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminConsentDTO;
import io.github.susimsek.springauthserversamples.mapper.AuthorizationServerMapperSupport;
import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AdminConsentServiceTest {
    @Mock private AdminUserService adminUserService;
    @Mock private AuthorizationConsentRepository authorizationConsentRepository;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationServerMapperSupport mapperSupport;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void returnsConsentsWithClientNamesAndAuthorities() {
        AuthorizationConsentEntity knownClient = consent("client-one", "alice", "openid profile");
        AuthorizationConsentEntity missingClient = consent("client-two", "bob", "email");
        RegisteredClientEntity client = new RegisteredClientEntity();
        client.setId("client-one");
        client.setClientName("Client One");
        Pageable pageable = Pageable.unpaged();
        when(authorizationConsentRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(knownClient, missingClient)));
        when(clientRepository.findAllById(List.of("client-one", "client-two")))
                .thenReturn(List.of(client));
        UserEntity alice = new UserEntity();
        alice.setId(1L);
        alice.setUsername("alice");
        UserEntity bob = new UserEntity();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userRepository.findAllByUsernameIn(List.of("alice", "bob")))
                .thenReturn(List.of(alice, bob));
        when(mapperSupport.readAuthorities("openid profile"))
                .thenReturn(
                        Set.of(
                                new SimpleGrantedAuthority("openid"),
                                new SimpleGrantedAuthority("profile")));
        when(mapperSupport.readAuthorities("email"))
                .thenReturn(Set.of(new SimpleGrantedAuthority("email")));

        List<AdminConsentDTO> result =
                service().consents("  alice  ", "", "", "", pageable).getContent();

        assertThat(result)
                .containsExactly(
                        new AdminConsentDTO(
                                "client-one",
                                "Client One",
                                "alice",
                                1L,
                                Set.of("openid", "profile"),
                                java.time.Instant.parse("2026-08-21T00:00:00Z"),
                                java.time.Instant.parse("2026-08-21T00:00:00Z")),
                        new AdminConsentDTO(
                                "client-two",
                                "client-two",
                                "bob",
                                2L,
                                Set.of("email"),
                                java.time.Instant.parse("2026-08-21T00:00:00Z"),
                                java.time.Instant.parse("2026-08-21T00:00:00Z")));
        verify(authorizationConsentRepository)
                .findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        org.mockito.ArgumentMatchers.eq(pageable));
        verify(clientRepository).findAllById(List.of("client-one", "client-two"));
        verify(mapperSupport).readAuthorities("openid profile");
        verify(mapperSupport).readAuthorities("email");
    }

    @Test
    void returnsEmptyConsentsWithoutLoadingClients() {
        Pageable pageable = Pageable.unpaged();
        when(authorizationConsentRepository.findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(Page.empty(pageable));

        assertThat(service().consents(null, "", "", "", pageable).getContent()).isEmpty();

        verify(authorizationConsentRepository)
                .findAll(
                        any(org.springframework.data.jpa.domain.Specification.class),
                        org.mockito.ArgumentMatchers.eq(pageable));
        verify(clientRepository).findAllById(List.of());
        verifyNoInteractions(mapperSupport);
    }

    @Test
    void revokesConsentAndRelatedAuthorizations() {
        AuthorizationConsentId id = new AuthorizationConsentId("client", "user");
        when(authorizationConsentRepository.existsById(id)).thenReturn(true);

        service().revokeConsent("client", "user", "admin");

        verify(adminUserService).assertCanManageUsername("user", "admin");
        verify(authorizationConsentRepository).deleteById(id);
        verify(authorizationRepository)
                .deleteByPrincipalNameAndRegisteredClientId("user", "client");
        verify(adminAuditEventService).record("consent.revoked", "consent", "client:user");
    }

    @Test
    void rejectsRevokingUnknownConsentWithoutDeletingAnything() {
        AuthorizationConsentId id = new AuthorizationConsentId("client", "user");
        when(authorizationConsentRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service().revokeConsent("client", "user", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Consent not found");

        verify(adminUserService).assertCanManageUsername("user", "admin");
        verify(authorizationConsentRepository).existsById(id);
        verifyNoInteractions(authorizationRepository, adminAuditEventService);
    }

    private static AuthorizationConsentEntity consent(
            String clientId, String principalName, String authorities) {
        AuthorizationConsentEntity entity =
                new AuthorizationConsentEntity(
                        new AuthorizationConsentId(clientId, principalName), authorities);
        entity.setCreatedAt(java.time.Instant.parse("2026-08-21T00:00:00Z"));
        entity.setUpdatedAt(java.time.Instant.parse("2026-08-21T00:00:00Z"));
        return entity;
    }

    private AdminConsentService service() {
        return new AdminConsentService(
                adminUserService,
                authorizationConsentRepository,
                authorizationRepository,
                clientRepository,
                userRepository,
                mapperSupport,
                adminAuditEventService);
    }
}
