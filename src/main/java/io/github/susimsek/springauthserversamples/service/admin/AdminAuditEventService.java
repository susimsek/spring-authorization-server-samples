package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventDTO;
import io.github.susimsek.springauthserversamples.repository.AdminEventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<AdminEventDTO> events(
            String q,
            String action,
            String targetType,
            String targetId,
            Instant from,
            Instant to,
            Pageable pageable) {
        String search = q == null ? "" : q.trim().toLowerCase();
        return adminEventRepository
                .findAll(
                        (root, query, cb) -> {
                            var predicate = cb.conjunction();
                            if (!search.isBlank()) {
                                String like = "%" + search + "%";
                                predicate =
                                        cb.and(
                                                predicate,
                                                cb.or(
                                                        cb.like(cb.lower(root.get("actor")), like),
                                                        cb.like(cb.lower(root.get("action")), like),
                                                        cb.like(
                                                                cb.lower(root.get("targetType")),
                                                                like),
                                                        cb.like(
                                                                cb.lower(root.get("targetId")),
                                                                like)));
                            }
                            if (action != null && !action.isBlank()) {
                                predicate = cb.and(predicate, cb.equal(root.get("action"), action));
                            }
                            if (targetType != null && !targetType.isBlank()) {
                                predicate =
                                        cb.and(
                                                predicate,
                                                cb.equal(root.get("targetType"), targetType));
                            }
                            if (targetId != null && !targetId.isBlank()) {
                                predicate =
                                        cb.and(predicate, cb.equal(root.get("targetId"), targetId));
                            }
                            if (from != null) {
                                predicate =
                                        cb.and(
                                                predicate,
                                                cb.greaterThanOrEqualTo(
                                                        root.get("occurredAt"), from));
                            }
                            if (to != null) {
                                predicate =
                                        cb.and(
                                                predicate,
                                                cb.lessThanOrEqualTo(root.get("occurredAt"), to));
                            }
                            return predicate;
                        },
                        pageable)
                .map(AdminAuditEventService::eventDTO);
    }

    public Page<AdminEventDTO> userEvents(Long userId, Pageable pageable) {
        return adminEventRepository
                .findByTargetTypeAndTargetId("user", userId.toString(), pageable)
                .map(AdminAuditEventService::eventDTO);
    }

    public Page<AdminEventDTO> clientEvents(String clientId, Pageable pageable) {
        return adminEventRepository
                .findByTargetTypeAndTargetId("client", clientId, pageable)
                .map(AdminAuditEventService::eventDTO);
    }

    private static AdminEventDTO eventDTO(AdminEventEntity event) {
        return new AdminEventDTO(
                event.getId(),
                event.getActor(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetId(),
                event.getOccurredAt());
    }

    public void avatarUpdated(Long userId) {
        record("user.avatar.updated", "user", userId.toString());
    }

    public void avatarDeleted(Long userId) {
        record("user.avatar.deleted", "user", userId.toString());
    }
}
