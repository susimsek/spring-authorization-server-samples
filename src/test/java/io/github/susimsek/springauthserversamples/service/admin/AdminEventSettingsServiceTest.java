package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AdminEventSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.AdminEventSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdminEventSettingsServiceTest {

    private final AdminEventSettingsRepository repository =
            org.mockito.Mockito.mock(AdminEventSettingsRepository.class);
    private final AdminAuditEventService auditEventService =
            org.mockito.Mockito.mock(AdminAuditEventService.class);
    private final AdminEventSettingsService service =
            new AdminEventSettingsService(repository, auditEventService);

    @Test
    void returnsAndUpdatesEventSettings() {
        AdminEventSettingsEntity entity = settings();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThat(service.get().eventsEnabled()).isTrue();

        var updated = new AdminEventSettingsRequestDTO(false, true, false, 30);
        assertThat(service.update(updated))
                .satisfies(
                        value -> {
                            assertThat(value.eventsEnabled()).isFalse();
                            assertThat(value.adminEventsDetailsEnabled()).isFalse();
                            assertThat(value.eventsExpirationDays()).isEqualTo(30);
                        });
        verify(repository).save(entity);
        verify(auditEventService).record("events.settings.updated", "event-settings", "default");
    }

    private static AdminEventSettingsEntity settings() {
        AdminEventSettingsEntity entity = new AdminEventSettingsEntity();
        entity.setId(1L);
        entity.setEventsEnabled(true);
        entity.setAdminEventsEnabled(true);
        entity.setAdminEventsDetailsEnabled(true);
        entity.setEventsExpirationDays(0);
        return entity;
    }
}
