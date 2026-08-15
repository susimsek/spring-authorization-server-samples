package io.github.susimsek.springauthserversamples.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DomainUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @Test
    void loadsEnabledUser() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(
                        Optional.of(
                                user(true, AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER)));

        var userDetails = new DomainUserDetailsService(userRepository).loadUserByUsername("admin");

        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
    }

    @Test
    void loadsDisabledUser() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user(false, AuthoritiesConstants.ADMIN)));

        var userDetails = new DomainUserDetailsService(userRepository).loadUserByUsername("admin");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void rejectsMissingUser() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                new DomainUserDetailsService(userRepository)
                                        .loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("user not found");
    }

    private static UserEntity user(boolean enabled, String... authorities) {
        Set<AuthorityEntity> authoritySet = new HashSet<>();
        for (int i = 0; i < authorities.length; i++) {
            authoritySet.add(new AuthorityEntity((long) i + 1, authorities[i]));
        }
        return new UserEntity(1L, "admin", "hash", enabled, authoritySet);
    }
}
