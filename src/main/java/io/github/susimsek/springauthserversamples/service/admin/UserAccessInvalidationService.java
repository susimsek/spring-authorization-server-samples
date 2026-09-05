package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.service.SessionInvalidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Invalidates a user's browser sessions and OAuth2 authorizations after an access change. */
@Service
@RequiredArgsConstructor
public class UserAccessInvalidationService {

    private final SessionInvalidationService sessionInvalidationService;

    public void invalidate(String username) {
        sessionInvalidationService.invalidatePrincipal(username);
    }

    public void invalidateOtherSessions(String username, String currentSessionId) {
        if (currentSessionId == null) {
            invalidate(username);
            return;
        }
        sessionInvalidationService.invalidatePrincipalExceptSession(username, currentSessionId);
    }
}
