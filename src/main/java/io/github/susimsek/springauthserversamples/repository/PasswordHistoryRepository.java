package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.PasswordHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {

    List<PasswordHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    long deleteByUserId(Long userId);
}
