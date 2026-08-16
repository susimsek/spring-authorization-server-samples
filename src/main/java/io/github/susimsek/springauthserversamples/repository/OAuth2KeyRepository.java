package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.OAuth2KeyEntity;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OAuth2KeyRepository extends JpaRepository<OAuth2KeyEntity, String> {

    String OAUTH2_KEYS_CACHE = "oauth2Keys";

    @Cacheable(cacheNames = OAUTH2_KEYS_CACHE)
    @Query("select k from OAuth2KeyEntity k order by k.createdAt desc")
    List<OAuth2KeyEntity> findAllKeys();
}
