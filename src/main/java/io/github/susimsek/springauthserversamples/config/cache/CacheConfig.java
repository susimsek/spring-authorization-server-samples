package io.github.susimsek.springauthserversamples.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
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
import java.util.OptionalLong;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final ApplicationProperties applicationProperties;

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(buildCaffeineConfig(cacheProperties()));
        return cacheManager;
    }

    private ApplicationProperties.Caffeine cacheProperties() {
        return applicationProperties.getCache().getCaffeine();
    }

    private Caffeine<Object, Object> buildCaffeineConfig(ApplicationProperties.Caffeine config) {
        return Caffeine.newBuilder()
                .expireAfterWrite(config.getTtl())
                .initialCapacity(config.getInitialCapacity())
                .maximumSize(config.getMaximumSize())
                .recordStats();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            name = "spring.jpa.properties.hibernate.cache.use_second_level_cache",
            havingValue = "true")
    @RequiredArgsConstructor
    static class HibernateSecondLevelCacheConfiguration {

        private final ApplicationProperties applicationProperties;

        @Bean
        CacheManager jcacheManager(JCacheManagerCustomizer customizer) {
            CacheManager manager =
                    Caching.getCachingProvider(CaffeineCachingProvider.class.getName())
                            .getCacheManager();
            customizer.customize(manager);
            return manager;
        }

        @Bean
        HibernatePropertiesCustomizer hibernatePropertiesCustomizer(CacheManager jcacheManager) {
            return properties -> properties.put(ConfigSettings.CACHE_MANAGER, jcacheManager);
        }

        @Bean
        JCacheManagerCustomizer cacheManagerCustomizer() {
            return cacheManager -> {
                createCache(cacheManager, AuthorizationConsentEntity.class.getName());
                createCache(cacheManager, AuthorizationEntity.class.getName());
                createCache(cacheManager, AuthorityEntity.class.getName());
                createCache(cacheManager, OAuth2KeyEntity.class.getName());
                createCache(cacheManager, RegisteredClientEntity.class.getName());
                createCache(cacheManager, UserEntity.class.getName());
                createCache(cacheManager, UserEntity.class.getName() + ".authorities");
                createCache(cacheManager, ClientRepository.REGISTERED_CLIENT_BY_CLIENT_ID_CACHE);
                createCache(cacheManager, OAuth2KeyRepository.OAUTH2_KEYS_CACHE);
                createCache(cacheManager, UserRepository.USER_BY_USERNAME_CACHE);
            };
        }

        private void createCache(CacheManager cacheManager, String cacheName) {
            Cache<Object, Object> cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                return;
            }
            ApplicationProperties.Caffeine config = applicationProperties.getCache().getCaffeine();
            CaffeineConfiguration<Object, Object> caffeineConfig = new CaffeineConfiguration<>();
            caffeineConfig.setMaximumSize(OptionalLong.of(config.getMaximumSize()));
            caffeineConfig.setExpireAfterWrite(OptionalLong.of(config.getTtl().toNanos()));
            caffeineConfig.setStatisticsEnabled(true);
            cacheManager.createCache(cacheName, caffeineConfig);
        }
    }
}
