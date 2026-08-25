package io.github.susimsek.springauthserversamples.service.admin;

import static org.mockito.Mockito.verify;

import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccessInvalidationServiceTest {

    @Mock private UserSessionRepository userSessionRepository;
    @Mock private AuthorizationRepository authorizationRepository;

    @Test
    void invalidateDeletesBrowserSessionsAndOAuth2Authorizations() {
        new UserAccessInvalidationService(userSessionRepository, authorizationRepository)
                .invalidate("alice");

        verify(userSessionRepository).deleteByPrincipalName("alice");
        verify(authorizationRepository).deleteByPrincipalName("alice");
    }
}
