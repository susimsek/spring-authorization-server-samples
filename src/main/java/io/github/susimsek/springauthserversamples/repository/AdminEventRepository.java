package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdminEventRepository
        extends JpaRepository<AdminEventEntity, String>,
                JpaSpecificationExecutor<AdminEventEntity> {
    Page<AdminEventEntity> findByTargetTypeAndTargetId(
            String targetType, String targetId, Pageable pageable);
}
