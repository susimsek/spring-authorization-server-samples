package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    String USER_BY_USERNAME_CACHE = "usersByUsername";

    @EntityGraph(value = "User.withAuthorities")
    @Cacheable(cacheNames = USER_BY_USERNAME_CACHE)
    Optional<UserEntity> findByUsername(String username);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByUsernameContainingIgnoreCaseAndEnabled(
            String username, boolean enabled, Pageable pageable);

    long countByAuthoritiesName(String authorityName);

    long countByAuthoritiesId(Long authorityId);
}
