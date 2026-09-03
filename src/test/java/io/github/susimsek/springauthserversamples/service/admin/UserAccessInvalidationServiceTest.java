package io.github.susimsek.springauthserversamples.service.admin;

import static org.mockito.Mockito.verify;

import io.github.susimsek.springauthserversamples.service.SessionInvalidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAccessInvalidationServiceTest {

    @Mock private SessionInvalidationService sessionInvalidationService;

    @Test
    void invalidateDeletesBrowserSessionsAndOAuth2Authorizations() {
        new UserAccessInvalidationService(sessionInvalidationService).invalidate("alice");

        verify(sessionInvalidationService).invalidatePrincipal("alice");
    }
}
