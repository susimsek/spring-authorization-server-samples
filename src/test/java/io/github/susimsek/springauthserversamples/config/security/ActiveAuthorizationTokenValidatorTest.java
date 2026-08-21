package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.repository.AuthorizationRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class ActiveAuthorizationTokenValidatorTest {

    @Mock private AuthorizationRepository authorizationRepository;

    @Test
    void acceptsAnAccessTokenWithAnActiveAuthorization() {
        when(authorizationRepository.findByAccessTokenValue("access-token"))
                .thenReturn(Optional.of(new AuthorizationEntity()));

        assertThat(validator().validate(token()).hasErrors()).isFalse();
    }

    @Test
    void rejectsAnAccessTokenAfterItsAuthorizationIsRemoved() {
        when(authorizationRepository.findByAccessTokenValue("access-token"))
                .thenReturn(Optional.empty());

        assertThat(validator().validate(token()).hasErrors()).isTrue();
    }

    private ActiveAuthorizationTokenValidator validator() {
        return new ActiveAuthorizationTokenValidator(authorizationRepository);
    }

    private static Jwt token() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .subject("admin")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}
