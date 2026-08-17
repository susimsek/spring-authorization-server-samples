package io.github.susimsek.springauthserversamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.domain.OAuth2KeyEntity;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
import io.github.susimsek.springauthserversamples.repository.OAuth2KeyRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.cache.CacheManager;
import javax.cache.Caching;
import org.hibernate.cache.jcache.ConfigSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class CacheConfigTest {

    @AfterEach
    void tearDown() {
        Caching.getCachingProvider(CaffeineCachingProvider.class.getName()).close();
    }

    @Test
    void usesDefaultCaffeineProperties() {
        ApplicationProperties.Caffeine caffeine =
                new ApplicationProperties.Caffeine(Duration.ofHours(1), 500, 1000L);

        assertThat(caffeine.ttl()).isEqualTo(Duration.ofHours(1));
        assertThat(caffeine.initialCapacity()).isEqualTo(500);
        assertThat(caffeine.maximumSize()).isEqualTo(1000L);
    }

    @Test
    void createsSpringCacheManagerFromApplicationProperties() {
        org.springframework.cache.CacheManager cacheManager =
                new CacheConfig(applicationProperties()).cacheManager();

        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
        Cache cache = cacheManager.getCache("users");
        assertThat(cache).isNotNull();
        cache.put("key", "value");
        assertThat(cache.get("key", String.class)).isEqualTo("value");
    }

    @Test
    void registersHibernateSecondLevelCacheRegions() {
        CacheConfig.HibernateSecondLevelCacheConfiguration configuration =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(applicationProperties());
        JCacheManagerCustomizer customizer = configuration.cacheManagerCustomizer();

        CacheManager cacheManager = configuration.jcacheManager(customizer);
        assertThat(cacheManager.getCache(AuthorizationConsentEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(AuthorizationEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(AuthorityEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(OAuth2KeyEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(RegisteredClientEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(UserEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(UserEntity.class.getName() + ".authorities")).isNotNull();
        assertThat(cacheManager.getCache(ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE))
                .isNotNull();
        assertThat(cacheManager.getCache(OAuth2KeyRepository.OAUTH2_KEYS_CACHE)).isNotNull();
        assertThat(cacheManager.getCache(UserRepository.USER_BY_USERNAME_CACHE)).isNotNull();
    }

    @Test
    void exposesJCacheManagerToHibernate() {
        CacheConfig.HibernateSecondLevelCacheConfiguration configuration =
                new CacheConfig.HibernateSecondLevelCacheConfiguration(applicationProperties());
        CacheManager cacheManager =
                Caching.getCachingProvider(CaffeineCachingProvider.class.getName())
                        .getCacheManager();
        HibernatePropertiesCustomizer customizer =
                configuration.hibernatePropertiesCustomizer(cacheManager);
        Map<String, Object> properties = new HashMap<>();

        customizer.customize(properties);

        assertThat(properties).containsEntry(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    private static ApplicationProperties applicationProperties() {
        return new ApplicationProperties(
                new ApplicationProperties.Cache(
                        new ApplicationProperties.Caffeine(Duration.ofMinutes(5), 10, 100)),
                new ApplicationProperties.Session("0 * * * * *"),
                new ApplicationProperties.AuthorizationServer("https://issuer.example"));
    }
}
