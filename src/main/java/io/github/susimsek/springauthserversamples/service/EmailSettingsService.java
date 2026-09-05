package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.EmailSettingsEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEmailSettingsDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEmailSettingsRequestDTO;
import io.github.susimsek.springauthserversamples.repository.EmailSettingsRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailSettingsService {
    private static final long SETTINGS_ID = 1L;
    private final EmailSettingsRepository repository;
    private final AdminAuditEventService auditEventService;

    @Transactional(readOnly = true)
    public AdminEmailSettingsDTO get() {
        EmailSettingsEntity e = entity();
        return new AdminEmailSettingsDTO(
                e.isEnabled(),
                e.getFromAddress(),
                e.getBaseUrl(),
                e.getHost(),
                e.getPort(),
                e.getUsername(),
                e.getPassword() != null && !e.getPassword().isBlank(),
                e.isSmtpAuth(),
                e.isStarttls(),
                e.isSsl());
    }

    @Transactional(readOnly = true)
    public EmailConfiguration current() {
        EmailSettingsEntity e = entity();
        return new EmailConfiguration(
                e.isEnabled(),
                e.getFromAddress(),
                e.getBaseUrl(),
                e.getHost(),
                e.getPort(),
                e.getUsername(),
                e.getPassword(),
                e.isSmtpAuth(),
                e.isStarttls(),
                e.isSsl());
    }

    @Transactional
    public AdminEmailSettingsDTO update(AdminEmailSettingsRequestDTO request) {
        EmailSettingsEntity e = entity();
        e.setEnabled(request.enabled());
        e.setFromAddress(request.fromAddress().trim());
        e.setBaseUrl(request.baseUrl().trim());
        e.setHost(request.host().trim());
        e.setPort(request.port());
        e.setUsername(request.username());
        e.setSmtpAuth(request.smtpAuth());
        e.setStarttls(request.starttls());
        e.setSsl(request.ssl());
        if (request.password() != null && !request.password().isBlank()) {
            e.setPassword(request.password());
        }
        repository.save(e);
        auditEventService.record("email.settings.updated", "email-settings", "default");
        return get();
    }

    private EmailSettingsEntity entity() {
        return repository
                .findById(SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException("Email settings are not initialized"));
    }

    public record EmailConfiguration(
            boolean enabled,
            String fromAddress,
            String baseUrl,
            String host,
            int port,
            String username,
            String password,
            boolean smtpAuth,
            boolean starttls,
            boolean ssl) {}
}
