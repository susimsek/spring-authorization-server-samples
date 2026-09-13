package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AdminEventSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminEventSettingsRepository
        extends JpaRepository<AdminEventSettingsEntity, Long> {}
