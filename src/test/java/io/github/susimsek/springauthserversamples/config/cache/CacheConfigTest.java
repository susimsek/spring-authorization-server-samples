package io.github.susimsek.springauthserversamples.config.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationConsentEntity;
import io.github.susimsek.springauthserversamples.domain.AuthorizationEntity;
import io.github.susimsek.springauthserversamples.domain.RegisteredClientEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.ClientRepository;
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
                new ApplicationProperties().getCache().getCaffeine();

        assertThat(caffeine.getTtl()).isEqualTo(Duration.ofHours(1));
        assertThat(caffeine.getInitialCapacity()).isEqualTo(500);
        assertThat(caffeine.getMaximumSize()).isEqualTo(1000L);
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

        assertThat(cacheManager.getCache("default-update-timestamps-region")).isNotNull();
        assertThat(cacheManager.getCache("default-query-results-region")).isNotNull();
        assertThat(cacheManager.getCache(AuthorizationConsentEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(AuthorizationEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(AuthorityEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(RegisteredClientEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(UserEntity.class.getName())).isNotNull();
        assertThat(cacheManager.getCache(UserEntity.class.getName() + ".authorities")).isNotNull();
        assertThat(cacheManager.getCache(ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE))
                .isNotNull();
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
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getCache().getCaffeine().setTtl(Duration.ofMinutes(5));
        applicationProperties.getCache().getCaffeine().setInitialCapacity(10);
        applicationProperties.getCache().getCaffeine().setMaximumSize(100);
        return applicationProperties;
    }
}
