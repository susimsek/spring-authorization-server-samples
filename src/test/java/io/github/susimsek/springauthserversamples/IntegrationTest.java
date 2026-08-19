package io.github.susimsek.springauthserversamples;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureMockMvc
@SpringBootTest(
        classes = SpringAuthorizationServerSamplesApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public @interface IntegrationTest {}
