package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AdminEventSettingsEntity;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminEventSettingsRepository
        extends JpaRepository<AdminEventSettingsEntity, Long> {

    String ADMIN_EVENT_SETTINGS_BY_ID_CACHE = "adminEventSettingsById";

    @Override
    @Cacheable(cacheNames = ADMIN_EVENT_SETTINGS_BY_ID_CACHE, key = "#id")
    Optional<AdminEventSettingsEntity> findById(Long id);
}
