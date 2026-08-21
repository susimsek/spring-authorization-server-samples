package io.github.susimsek.springauthserversamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
}
