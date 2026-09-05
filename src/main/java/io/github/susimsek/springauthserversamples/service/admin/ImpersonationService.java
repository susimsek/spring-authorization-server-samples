package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.ImpersonationTicketEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminImpersonationDTO;
import io.github.susimsek.springauthserversamples.mapper.AdminImpersonationMapper;
import io.github.susimsek.springauthserversamples.mapper.AdminImpersonationTicketMapper;
import io.github.susimsek.springauthserversamples.repository.ImpersonationTicketRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class ImpersonationService {

    private static final Duration TICKET_LIFESPAN = Duration.ofSeconds(60);
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final ImpersonationTicketRepository ticketRepository;
    private final AdminAuditEventService auditEventService;
    private final AdminImpersonationMapper adminImpersonationMapper;
    private final AdminImpersonationTicketMapper adminImpersonationTicketMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AdminImpersonationDTO issue(Long targetId, String actorUsername) {
        UserEntity target =
                userRepository
                        .findById(targetId)
                        .orElseThrow(() -> ApiException.notFound("User not found"));
        if (target.getUsername().equals(actorUsername)) {
            throw ApiException.badRequest(
                    ApiErrorCode.USER_PROTECTED, "You cannot impersonate yourself");
        }
        if (!target.isEnabled()) {
            throw ApiException.badRequest(
                    ApiErrorCode.USER_PROTECTED, "Disabled users cannot be impersonated");
        }
        if (userDetailsService.loadUserByUsername(target.getUsername()).getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw ApiException.forbidden(
                    ApiErrorCode.USER_PROTECTED, "Administrators cannot be impersonated");
        }
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String rawTicket = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        Instant issuedAt = Instant.now();
        ImpersonationTicketEntity ticket =
                adminImpersonationTicketMapper.toEntity(
                        hash(rawTicket),
                        actorUsername,
                        target,
                        issuedAt,
                        issuedAt.plus(TICKET_LIFESPAN));
        ticketRepository.save(ticket);
        auditEventService.record("user.impersonation.started", "user", targetId.toString());
        return adminImpersonationMapper.toDTO(
                "/impersonation/accept", target.getUsername(), rawTicket);
    }

    @Transactional
    public UserEntity consume(String rawTicket, String actorUsername) {
        if (rawTicket == null || rawTicket.isBlank()) {
            throw ApiException.badRequest(
                    ApiErrorCode.INVALID_REQUEST, "Impersonation ticket is required");
        }
        ImpersonationTicketEntity ticket =
                ticketRepository
                        .findByTicketHash(hash(rawTicket))
                        .orElseThrow(
                                () ->
                                        ApiException.badRequest(
                                                ApiErrorCode.INVALID_REQUEST,
                                                "Impersonation ticket is invalid"));
        if (!ticket.getActorUsername().equals(actorUsername)
                || ticket.getConsumedAt() != null
                || !ticket.getExpiresAt().isAfter(Instant.now())) {
            throw ApiException.badRequest(
                    ApiErrorCode.INVALID_REQUEST, "Impersonation ticket is invalid");
        }
        ticket.setConsumedAt(Instant.now());
        UserEntity target = ticket.getTargetUser();
        target.getId();
        target.getUsername();
        return target;
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
