package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Invalidates a user's browser sessions and OAuth2 authorizations after an access change. */
@Service
@RequiredArgsConstructor
public class UserAccessInvalidationService {

    private final UserSessionRepository userSessionRepository;
    private final AuthorizationRepository authorizationRepository;

    public void invalidate(String username) {
        userSessionRepository.deleteByPrincipalName(username);
        authorizationRepository.deleteByPrincipalName(username);
    }
}
