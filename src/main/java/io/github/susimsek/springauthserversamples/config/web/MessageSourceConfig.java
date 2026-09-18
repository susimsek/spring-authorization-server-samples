package io.github.susimsek.springauthserversamples.config.web;

import io.github.susimsek.springauthserversamples.repository.LocalizationMessageOverrideRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration(proxyBeanMethods = false)
public class MessageSourceConfig {

    @Bean(name = "bundledMessageSource")
    MessageSource bundledMessageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:/i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean(name = "messageSource")
    @Primary
    MessageSource messageSource(
            @Qualifier("bundledMessageSource") MessageSource bundledMessageSource,
            LocalizationMessageOverrideRepository overrideRepository) {
        return new DatabaseMessageSource(bundledMessageSource, overrideRepository);
    }
}
