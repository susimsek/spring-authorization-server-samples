package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.susimsek.springauthserversamples.config.ApplicationProperties;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.LoginSettingsService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsRepository;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;

class WebAuthnConfigTest {

    private final WebAuthnConfig config = new WebAuthnConfig();

    @Test
    void createsWebAuthnInfrastructureBeans() {
        PublicKeyCredentialUserEntityRepository users =
                config.webAuthnUserEntityRepository(mock(UserRepository.class));
        UserCredentialRepository credentials =
                config.webAuthnUserCredentialRepository(mock(JdbcOperations.class));

        assertThat(users).isNotNull();
        assertThat(credentials).isNotNull();
        assertThat(config.webAuthnRequestOptionsRepository())
                .isInstanceOf(PublicKeyCredentialRequestOptionsRepository.class);
        WebAuthnRelyingPartyOperations operations =
                config.webAuthnRelyingPartyOperations(
                        users,
                        credentials,
                        new ApplicationProperties(),
                        mock(LoginSettingsService.class));
        assertThat(operations).isNotNull();
        AuthenticationManager manager =
                config.webAuthnAuthenticationManager(operations, mock(UserDetailsService.class));
        assertThat(manager).isNotNull();
    }

    @Test
    void mapsConfiguredWebAuthnValues() throws Exception {
        assertThat(invoke("csv", String.class, " ES256, ,RS256 "))
                .isEqualTo(List.of("ES256", "RS256"));
        assertThat(invoke("csv", String.class, (Object) null)).isEqualTo(List.of());
        assertThat(invoke("value", String.class, (Object) null)).isEqualTo("");
        assertThat(invoke("value", String.class, "  rp  ")).isEqualTo("rp");

        @SuppressWarnings("unchecked")
        List<PublicKeyCredentialParameters> algorithms =
                (List<PublicKeyCredentialParameters>)
                        invoke(
                                "algorithms",
                                String.class,
                                "EdDSA,ES256,ES384,ES512,RS1,RS256,RS384,RS512");
        assertThat(algorithms).hasSize(8);
        assertThat(value(invoke("attachment", String.class, "platform"))).isEqualTo("platform");
        assertThat(value(invoke("attachment", String.class, "cross-platform")))
                .isEqualTo("cross-platform");
        assertThat(invoke("attachment", String.class, "any")).isNull();
        assertThat(value(invoke("residentKey", String.class, "required")))
                .isEqualToIgnoringCase("required");
        assertThat(value(invoke("userVerification", String.class, "discouraged")))
                .isEqualToIgnoringCase("discouraged");
        assertThat(value(invoke("userVerification", String.class, "preferred")))
                .isEqualToIgnoringCase("preferred");
        assertThat(value(invoke("userVerification", String.class, "anything")))
                .isEqualToIgnoringCase("required");
        assertThat(value(invoke("attestation", String.class, "direct")))
                .isEqualToIgnoringCase("direct");
        assertThatThrownBy(() -> invoke("algorithm", String.class, "unsupported"))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static Object invoke(String name, Class<?> parameterType, Object value)
            throws Exception {
        Class<?> operations =
                Class.forName(
                        "io.github.susimsek.springauthserversamples.config.security.WebAuthnConfig$ConfigurableWebAuthnRelyingPartyOperations");
        Method method = operations.getDeclaredMethod(name, parameterType);
        method.setAccessible(true);
        return method.invoke(null, value);
    }

    private static String value(Object option) throws Exception {
        return (String) option.getClass().getMethod("getValue").invoke(option);
    }
}
