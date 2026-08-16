package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(
                        user ->
                                User.withUsername(user.getUsername())
                                        .password(user.getPassword())
                                        .authorities(authorities(user))
                                        .disabled(!user.isEnabled())
                                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private static String[] authorities(UserEntity user) {
        return user.getAuthorities().stream().map(AuthorityEntity::getName).toArray(String[]::new);
    }
}
