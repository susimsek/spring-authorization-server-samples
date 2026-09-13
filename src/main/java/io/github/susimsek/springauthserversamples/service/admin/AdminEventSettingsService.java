package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AdminEventSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.AdminEventSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminEventSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final AdminEventSettingsRepository repository;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public AdminEventSettingsDTO get() {
        return toDTO(entity());
    }

    @Transactional
    public AdminEventSettingsDTO update(AdminEventSettingsRequestDTO request) {
        AdminEventSettingsEntity settings = entity();
        settings.setEventsEnabled(request.eventsEnabled());
        settings.setAdminEventsEnabled(request.adminEventsEnabled());
        settings.setAdminEventsDetailsEnabled(request.adminEventsDetailsEnabled());
        settings.setEventsExpirationDays(request.eventsExpirationDays());
        repository.save(settings);
        auditEventService.record("events.settings.updated", "event-settings", "default");
        return toDTO(settings);
    }

    private AdminEventSettingsEntity entity() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Event settings are not initialized"));
    }

    private static AdminEventSettingsDTO toDTO(AdminEventSettingsEntity settings) {
        return new AdminEventSettingsDTO(
                settings.isEventsEnabled(),
                settings.isAdminEventsEnabled(),
                settings.isAdminEventsDetailsEnabled(),
                settings.getEventsExpirationDays());
    }
}
