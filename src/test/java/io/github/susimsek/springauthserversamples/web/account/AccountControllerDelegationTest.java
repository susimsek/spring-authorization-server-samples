package io.github.susimsek.springauthserversamples.web.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.dto.account.AccountAvatarDTO;
import io.github.susimsek.springauthserversamples.service.UserProfileService;
import io.github.susimsek.springauthserversamples.service.account.AccountApplicationService;
import io.github.susimsek.springauthserversamples.service.account.AccountAvatarService;
import io.github.susimsek.springauthserversamples.service.account.AccountDeletionService;
import io.github.susimsek.springauthserversamples.service.account.AccountProfileService;
import io.github.susimsek.springauthserversamples.service.account.AccountSessionService;
import io.github.susimsek.springauthserversamples.service.account.MfaService;
import io.github.susimsek.springauthserversamples.service.account.RecoveryCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AccountControllerDelegationTest {

    private final AccountProfileService profileService = mock(AccountProfileService.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final AccountSessionService sessionService = mock(AccountSessionService.class);
    private final AccountApplicationService applicationService =
            mock(AccountApplicationService.class);
    private final AccountAvatarService avatarService = mock(AccountAvatarService.class);
    private final AccountDeletionService deletionService = mock(AccountDeletionService.class);
    private final MfaService mfaService = mock(MfaService.class);
    private final RecoveryCodeService recoveryCodeService = mock(RecoveryCodeService.class);

    @Test
    void delegatesAccountAvatarEndpoints() {
        var controller =
                new AccountController(
                        profileService,
                        userProfileService,
                        sessionService,
                        applicationService,
                        avatarService,
                        deletionService,
                        mfaService,
                        recoveryCodeService);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "alice", "ignored", java.util.List.of());
        var avatar = new AccountAvatarDTO("/avatars/alice?v=1");
        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});
        when(avatarService.avatar("alice")).thenReturn(avatar);
        when(avatarService.updateAvatar("alice", file)).thenReturn(avatar);

        assertThat(controller.avatar(authentication)).isSameAs(avatar);
        assertThat(controller.updateAvatar(authentication, file)).isSameAs(avatar);
        assertThat(controller.deleteAvatar(authentication).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        verify(avatarService).avatar("alice");
        verify(avatarService).updateAvatar("alice", file);
        verify(avatarService).deleteAvatar("alice");
    }
}
