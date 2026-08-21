package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.repository.AuthorizationConsentRepository;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthorizationConsentRepository authorizationConsentRepository;

    @Transactional(readOnly = true)
    public DashboardView dashboard() {
        return new DashboardView(
                clientRepository.count(),
                userRepository.count(),
                userSessionRepository.countByExpiryTimeAfter(Instant.now().toEpochMilli()),
                authorizationConsentRepository.count());
    }

    public record DashboardView(long clients, long users, long sessions, long consents) {}
}
