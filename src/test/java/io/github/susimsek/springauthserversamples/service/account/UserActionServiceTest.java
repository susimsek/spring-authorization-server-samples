package io.github.susimsek.springauthserversamples.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.UserAction;
import io.github.susimsek.springauthserversamples.domain.UserActionTokenEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserActionTokenRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.PasswordService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserActionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserActionTokenRepository tokenRepository;
    @Mock private PasswordService passwordService;
    @Mock private UserAccessInvalidationService invalidationService;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ApplicationProperties applicationProperties;

    private UserActionService service;

    @BeforeEach
    void setUp() {
        service =
                new UserActionService(
                        userRepository,
                        tokenRepository,
                        passwordService,
                        invalidationService,
                        auditEventService,
                        eventPublisher,
                        applicationProperties);
    }

    @Test
    void sendsPasswordResetWithoutPersistingTheRawToken() throws Exception {
        UserEntity user = user();
        when(applicationProperties.mail())
                .thenReturn(
                        new ApplicationProperties.Mail(
                                true, "no-reply@example.test", "http://127.0.0.1:9090"));
        when(userRepository.findIdByUsername("alice")).thenReturn(Optional.of(7L));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(user));
        when(tokenRepository.findFirstByUserIdAndActionOrderByIssuedAtDesc(
                        7L, UserAction.UPDATE_PASSWORD))
                .thenReturn(Optional.empty());

        service.forgotPassword("alice", Locale.ENGLISH);

        ArgumentCaptor<UserActionTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(UserActionTokenEntity.class);
        ArgumentCaptor<UserActionEmailEvent> eventCaptor =
                ArgumentCaptor.forClass(UserActionEmailEvent.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        String rawToken =
                org.springframework.web.util.UriComponentsBuilder.fromUriString(
                                eventCaptor.getValue().actionUrl())
                        .build()
                        .getQueryParams()
                        .getFirst("token");
        assertThat(rawToken).isNotBlank();
        assertThat(eventCaptor.getValue().actionUrl())
                .startsWith("http://127.0.0.1:9090/reset-password?");
        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(hash(rawToken));
        assertThat(tokenCaptor.getValue().getTokenHash()).doesNotContain(rawToken);
        assertThat(tokenCaptor.getValue().getExpiresAt())
                .isAfter(tokenCaptor.getValue().getIssuedAt().plusSeconds(43199));
    }

    @Test
    void forgotPasswordDoesNotRevealAnUnknownAccount() {
        when(applicationProperties.mail())
                .thenReturn(
                        new ApplicationProperties.Mail(
                                true, "no-reply@example.test", "http://localhost:9090"));
        when(userRepository.findIdByUsername("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());

        service.forgotPassword("missing", Locale.ENGLISH);

        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void verifiesEmailAndConsumesToken() throws Exception {
        UserActionTokenEntity token = token(UserAction.VERIFY_EMAIL, Instant.now().plusSeconds(60));
        when(tokenRepository.findUserIdByTokenHash(hash("raw-token"))).thenReturn(Optional.of(7L));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(token.getUser()));
        when(tokenRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));

        service.verifyEmail("raw-token");

        assertThat(token.getUser().isEmailVerified()).isTrue();
        assertThat(token.getConsumedAt()).isNotNull();
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        UserActionTokenEntity token = token(UserAction.VERIFY_EMAIL, Instant.now().minusSeconds(1));
        when(tokenRepository.findUserIdByTokenHash(hash("raw-token"))).thenReturn(Optional.of(7L));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(token.getUser()));
        when(tokenRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("raw-token"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.ACTION_TOKEN_EXPIRED);
    }

    @Test
    void resetsPasswordAndInvalidatesAllUserAccess() throws Exception {
        UserActionTokenEntity token =
                token(UserAction.UPDATE_PASSWORD, Instant.now().plusSeconds(60));
        when(tokenRepository.findUserIdByTokenHash(hash("raw-token"))).thenReturn(Optional.of(7L));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(token.getUser()));
        when(tokenRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            invocation.<UserEntity>getArgument(0).setPassword("encoded-password");
                            return null;
                        })
                .when(passwordService)
                .changePassword(any(UserEntity.class), org.mockito.Mockito.eq("new-password"));

        service.resetPassword("raw-token", "new-password");

        assertThat(token.getUser().getPassword()).isEqualTo("encoded-password");
        assertThat(token.getConsumedAt()).isNotNull();
        verify(invalidationService).invalidate("alice");
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setEnabled(true);
        user.setPassword("old-password");
        return user;
    }

    private static UserActionTokenEntity token(UserAction action, Instant expiresAt)
            throws Exception {
        UserActionTokenEntity token = new UserActionTokenEntity();
        token.setUser(user());
        token.setEmail("alice@example.test");
        token.setCredentialFingerprint(hash("old-password"));
        token.setAction(action);
        token.setIssuedAt(Instant.now().minusSeconds(10));
        token.setExpiresAt(expiresAt);
        return token;
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of()
                .formatHex(
                        MessageDigest.getInstance("SHA-256")
                                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
