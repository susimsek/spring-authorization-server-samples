package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.EmailSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSettingsRepository extends JpaRepository<EmailSettingsEntity, Long> {}
