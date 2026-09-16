package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;

class SecurityJsonMapperTest {

    @Test
    void preservesSecurityContextTypeWhenReadingAnUntypedSessionAttribute() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "N/A", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        SecurityJsonMapper mapper = new SecurityJsonMapper(getClass().getClassLoader());

        byte[] serialized = mapper.delegate().writeValueAsBytes(context);
        Object restored = mapper.delegate().readValue(serialized, Object.class);

        assertThat(restored).isInstanceOf(SecurityContext.class);
        assertThat(((SecurityContext) restored).getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void preservesWebauthnAuthenticationWhenReadingAnUntypedSessionAttribute() throws Exception {
        WebAuthnAuthentication authentication =
                new WebAuthnAuthentication(
                        ImmutablePublicKeyCredentialUserEntity.builder()
                                .name("admin")
                                .displayName("Admin")
                                .id(new Bytes(new byte[] {1, 2, 3}))
                                .build(),
                        AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityJsonMapper mapper = new SecurityJsonMapper(getClass().getClassLoader());

        byte[] serialized = mapper.delegate().writeValueAsBytes(authentication);
        Object restored = mapper.delegate().readValue(serialized, Object.class);

        assertThat(restored).isInstanceOf(WebAuthnAuthentication.class);
        assertThat(((WebAuthnAuthentication) restored).getName()).isEqualTo("admin");
    }
}
