package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.security.WebAuthnUserEntityRepository;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;

class WebAuthnServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCredentialRepository credentialRepository =
            mock(UserCredentialRepository.class);
    private final UserAccessInvalidationService invalidationService =
            mock(UserAccessInvalidationService.class);
    private final AdminAuditEventService auditEventService = mock(AdminAuditEventService.class);

    @Test
    void listsCredentialsNewestFirst() {
        UserEntity user = user();
        CredentialRecord first = mock(CredentialRecord.class);
        CredentialRecord second = mock(CredentialRecord.class);
        when(first.getCredentialId()).thenReturn(Bytes.random());
        when(second.getCredentialId()).thenReturn(Bytes.random());
        when(first.getCreated()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(second.getCreated()).thenReturn(Instant.parse("2026-01-02T00:00:00Z"));
        when(first.getLastUsed()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(second.getLastUsed()).thenReturn(Instant.parse("2026-01-02T00:00:00Z"));
        when(first.getTransports()).thenReturn(Set.of());
        when(second.getTransports()).thenReturn(Set.of());
        when(first.getLabel()).thenReturn("first");
        when(second.getLabel()).thenReturn("second");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(WebAuthnUserEntityRepository.userHandle(7L)))
                .thenReturn(List.of(first, second));

        var result = service().credentials("alice", PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0)).isNotNull();
    }

    @Test
    void deletesOnlyCredentialsOwnedByTheAccount() {
        UserEntity user = user();
        Bytes credentialId = Bytes.random();
        CredentialRecord record = mock(CredentialRecord.class);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(credentialRepository.findByCredentialId(credentialId)).thenReturn(record);
        when(record.getUserEntityUserId()).thenReturn(WebAuthnUserEntityRepository.userHandle(7L));

        service().delete("alice", credentialId.toBase64UrlString());

        verify(credentialRepository).delete(credentialId);
        verify(invalidationService).invalidate("alice");
        verify(auditEventService).record("account.passkey.deleted", "account", "7");
    }

    @Test
    void rejectsCredentialOwnedByAnotherAccount() {
        UserEntity user = user();
        CredentialRecord record = mock(CredentialRecord.class);
        Bytes credentialId = Bytes.random();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(credentialRepository.findByCredentialId(credentialId)).thenReturn(record);
        when(record.getUserEntityUserId()).thenReturn(WebAuthnUserEntityRepository.userHandle(8L));

        assertThatThrownBy(() -> service().delete("alice", credentialId.toBase64UrlString()))
                .isInstanceOf(ApiException.class);
    }

    private WebAuthnService service() {
        return new WebAuthnService(
                userRepository, credentialRepository, invalidationService, auditEventService);
    }

    private UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        return user;
    }
}
