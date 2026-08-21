package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import io.github.susimsek.springauthserversamples.repository.AdminEventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditEventService {

    private final AdminEventRepository adminEventRepository;

    public void record(String action, String targetType, String targetId) {
        AdminEventEntity event = new AdminEventEntity();
        event.setId(UUID.randomUUID().toString());
        event.setActor(
                java.util.Optional.ofNullable(
                                SecurityContextHolder.getContext().getAuthentication())
                        .filter(authentication -> authentication.isAuthenticated())
                        .map(authentication -> authentication.getName())
                        .orElse("system"));
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setOccurredAt(Instant.now());
        adminEventRepository.save(event);
    }

    public void avatarUpdated(Long userId) {
        record("user.avatar.updated", "user", userId.toString());
    }

    public void avatarDeleted(Long userId) {
        record("user.avatar.deleted", "user", userId.toString());
    }
}
