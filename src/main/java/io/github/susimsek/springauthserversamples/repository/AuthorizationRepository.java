package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuthorizationRepository
        extends JpaRepository<AuthorizationEntity, String>,
                JpaSpecificationExecutor<AuthorizationEntity> {}
