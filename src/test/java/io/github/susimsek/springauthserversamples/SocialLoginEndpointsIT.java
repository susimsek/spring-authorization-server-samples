package io.github.susimsek.springauthserversamples;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@TestPropertySource(properties = "app.social-login.enabled=true")
class SocialLoginEndpointsIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void publicProviderDiscoveryExposesProviderConfigurationState() throws Exception {
        mockMvc.perform(get("/api/auth/social-providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").isString())
                .andExpect(jsonPath("$[0].configured").isBoolean());
    }

    @Test
    void administratorCanReadSocialProviderSettingsWithoutReceivingSecrets() throws Exception {
        mockMvc.perform(
                        get("/api/admin/settings/social-providers")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").isString())
                .andExpect(jsonPath("$[0].clientSecretConfigured").isBoolean())
                .andExpect(jsonPath("$[0].clientSecret").doesNotExist());
    }
}
