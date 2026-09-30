package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserEventSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEventSettingsRepository extends JpaRepository<UserEventSettingsEntity, Long> {}
