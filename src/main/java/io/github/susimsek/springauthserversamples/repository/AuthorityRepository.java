package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {

    String AUTHORITY_BY_NAME_CACHE = "authoritiesByName";

    List<AuthorityEntity> findByNameIn(Iterable<String> names);

    Page<AuthorityEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByName(String name);

    @Cacheable(cacheNames = AUTHORITY_BY_NAME_CACHE, key = "#name")
    Optional<AuthorityEntity> findByName(String name);
}
