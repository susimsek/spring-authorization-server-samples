package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminEventRepository extends JpaRepository<AdminEventEntity, String> {}
