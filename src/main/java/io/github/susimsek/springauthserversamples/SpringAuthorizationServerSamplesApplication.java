package io.github.susimsek.springauthserversamples;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.config.aot.NativeRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
@ImportRuntimeHints(NativeRuntimeHints.class)
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class SpringAuthorizationServerSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAuthorizationServerSamplesApplication.class, args);
    }
}
