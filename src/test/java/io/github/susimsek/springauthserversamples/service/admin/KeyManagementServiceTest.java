package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.OAuth2KeyEntity;
import io.github.susimsek.springauthserversamples.repository.OAuth2KeyRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class KeyManagementServiceTest {
    @Mock private OAuth2KeyRepository keyRepository;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void filtersKeysByActiveState() {
        OAuth2KeyEntity key = key();
        PageRequest pageable = PageRequest.of(0, 20);
        when(keyRepository.findByKidContainingIgnoreCaseAndActive("current", true, pageable))
                .thenReturn(new PageImpl<>(List.of(key)));

        var keys = service().keys(" current ", true, pageable);

        assertThat(keys.getContent().getFirst().kid()).isEqualTo("current-key");
        verify(keyRepository).findByKidContainingIgnoreCaseAndActive("current", true, pageable);
    }

    @Test
    void listsAllKeysWhenActiveStateIsNotSpecified() {
        OAuth2KeyEntity key = key();
        PageRequest pageable = PageRequest.of(0, 20);
        when(keyRepository.findByKidContainingIgnoreCase("current", pageable))
                .thenReturn(new PageImpl<>(List.of(key)));

        var keys = service().keys(" current ", null, pageable);

        assertThat(keys.getContent().getFirst())
                .extracting(
                        KeyManagementService.KeyView::id,
                        KeyManagementService.KeyView::kid,
                        KeyManagementService.KeyView::active)
                .containsExactly("key-id", "current-key", true);
        verify(keyRepository).findByKidContainingIgnoreCase("current", pageable);
    }

    @Test
    void rotatesKeyAndDeactivatesExistingKeys() {
        OAuth2KeyEntity previousKey = key();
        when(keyRepository.findAllForRotation()).thenReturn(List.of(previousKey));
        when(keyRepository.save(org.mockito.ArgumentMatchers.any(OAuth2KeyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().rotateKey();

        assertThat(previousKey.isActive()).isFalse();
        assertThat(result)
                .extracting(
                        KeyManagementService.KeyView::type,
                        KeyManagementService.KeyView::algorithm,
                        KeyManagementService.KeyView::use,
                        KeyManagementService.KeyView::active)
                .containsExactly("RSA", "RS256", "sig", true);
        verify(keyRepository).save(org.mockito.ArgumentMatchers.any(OAuth2KeyEntity.class));
        verify(auditEventService).record("key.rotated", "key", result.id());
    }

    @Test
    void wrapsRotationFailuresInAdminException() {
        when(keyRepository.findAllForRotation()).thenThrow(new IllegalStateException("database"));

        assertThatThrownBy(() -> service().rotateKey())
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Could not rotate the signing key");
    }

    private KeyManagementService service() {
        return new KeyManagementService(keyRepository, auditEventService);
    }

    private static OAuth2KeyEntity key() {
        OAuth2KeyEntity key = new OAuth2KeyEntity();
        key.setId("key-id");
        key.setKid("current-key");
        key.setType("RSA");
        key.setAlgorithm("RS256");
        key.setUse("sig");
        key.setActive(true);
        key.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return key;
    }
}
