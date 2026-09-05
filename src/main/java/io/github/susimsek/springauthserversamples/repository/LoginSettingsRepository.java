package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSettingsRepository extends JpaRepository<LoginSettingsEntity, Long> {}
