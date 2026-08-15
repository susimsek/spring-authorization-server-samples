package io.github.susimsek.springauthserversamples;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class SpringAuthorizationServerSamplesApplicationMainTest {

    @Test
    void mainDelegatesToSpringApplication() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            SpringAuthorizationServerSamplesApplication.main(new String[] {"--test"});

            mocked.verify(
                    () ->
                            SpringApplication.run(
                                    SpringAuthorizationServerSamplesApplication.class,
                                    new String[] {"--test"}));
        }
    }
}
