package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.dto.admin.AdminEventDTO;
import io.github.susimsek.springauthserversamples.mapper.AdminEventMapper;
import io.github.susimsek.springauthserversamples.repository.AdminEventRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class AdminAuditEventService {

    private final AdminEventRepository adminEventRepository;
    private final AdminEventMapper adminEventMapper;

    public AdminAuditEventService(AdminEventRepository adminEventRepository) {
        this(adminEventRepository, Mappers.getMapper(AdminEventMapper.class));
    }

    public void record(String action, String targetType, String targetId) {
        String actor =
                java.util.Optional.ofNullable(
                                SecurityContextHolder.getContext().getAuthentication())
                        .filter(authentication -> authentication.isAuthenticated())
                        .map(authentication -> authentication.getName())
                        .orElse("system");
        adminEventRepository.save(
                adminEventMapper.toEntity(
                        UUID.randomUUID().toString(),
                        actor,
                        action,
                        targetType,
                        targetId,
                        Instant.now()));
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
                .map(adminEventMapper::toDTO);
    }

    public Page<AdminEventDTO> userEvents(Long userId, Pageable pageable) {
        return adminEventRepository
                .findByTargetTypeAndTargetId("user", userId.toString(), pageable)
                .map(adminEventMapper::toDTO);
    }

    public Page<AdminEventDTO> clientEvents(String clientId, Pageable pageable) {
        return adminEventRepository
                .findByTargetTypeAndTargetId("client", clientId, pageable)
                .map(adminEventMapper::toDTO);
    }

    public void avatarUpdated(Long userId) {
        record("user.avatar.updated", "user", userId.toString());
    }

    public void avatarDeleted(Long userId) {
        record("user.avatar.deleted", "user", userId.toString());
    }
}
