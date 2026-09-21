package io.github.susimsek.springauthserversamples.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.test.util.ReflectionTestUtils;

class PageableConfigTest {

    @Test
    void configuresSafePageableDefaults() {
        PageableHandlerMethodArgumentResolverCustomizer customizer =
                new PageableConfig().pageableHandlerMethodArgumentResolverCustomizer();
        PageableHandlerMethodArgumentResolver resolver =
                new PageableHandlerMethodArgumentResolver();

        customizer.customize(resolver);

        assertThat(ReflectionTestUtils.getField(resolver, "maxPageSize")).isEqualTo(100);
        assertThat(
                        ((org.springframework.data.domain.Pageable)
                                        ReflectionTestUtils.getField(resolver, "fallbackPageable"))
                                .getPageSize())
                .isEqualTo(20);
    }
}
