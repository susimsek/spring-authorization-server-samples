package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.domain.UserSessionEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminIdentityProviderRequestDTO;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserSessionRepository;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@TestPropertySource(
        properties = {
            "app.social-login.encryption-key=integration-test-social-login-key",
            "app.social-login.enabled=true"
        })
class AdminLifecycleEndpointsIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private RegisteredClientRepository registeredClientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserAvatarRepository userAvatarRepository;
    @Autowired private UserSessionRepository userSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final byte[] ONE_PIXEL_PNG =
            Base64.getDecoder()
                    .decode(
                            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/"
                                + "nW7J4QAAAABJRU5ErkJggg==");

    @Test
    void administratorCanCreateUpdateAssignAndDeleteRole() throws Exception {
        String role = "ROLE_IT_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        try {
            mockMvc.perform(
                            post("/api/admin/roles")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"name\":\""
                                                    + role
                                                    + "\",\"description\":\"IT role\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(role));

            mockMvc.perform(
                            put("/api/admin/roles/{name}", role)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"name\":\""
                                                    + role
                                                    + "\",\"description\":\"Updated\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Updated"));

            mockMvc.perform(
                            post("/api/admin/roles/{name}/users", role)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"userId\":2}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userCount").value(1));

            mockMvc.perform(delete("/api/admin/roles/{name}/users/2", role).with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userCount").value(0));
        } finally {
            mockMvc.perform(delete("/api/admin/roles/{name}", role).with(admin()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    void administratorCanAssignAndRemoveRequiredAction() throws Exception {
        mockMvc.perform(get("/api/admin/required-actions").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").isString());

        mockMvc.perform(post("/api/admin/required-actions/users/2/UPDATE_PROFILE").with(admin()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/required-actions/users/2").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'UPDATE_PROFILE')]").isNotEmpty());
        mockMvc.perform(delete("/api/admin/required-actions/users/2/UPDATE_PROFILE").with(admin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void administratorCanExerciseUserLifecycleOperations() throws Exception {
        String username = "admin-it-" + UUID.randomUUID();
        String response =
                mockMvc.perform(
                                post("/api/admin/users")
                                        .with(admin())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\"Admin-test12!\",\"enabled\":true,\"roles\":[\"ROLE_USER\"]}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = JSON.readTree(response).get("id").asLong();
        try {
            mockMvc.perform(
                            put("/api/admin/users/{id}/enabled", id)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"enabled\":false}"))
                    .andExpect(status().isNoContent());
            assertThat(userRepository.findById(id).orElseThrow().isEnabled()).isFalse();
            mockMvc.perform(
                            put("/api/admin/users/{id}/enabled", id)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"enabled\":true}"))
                    .andExpect(status().isNoContent());
            assertThat(userRepository.findById(id).orElseThrow().isEnabled()).isTrue();
            mockMvc.perform(
                            put("/api/admin/users/{id}/password", id)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"password\":\"Admin-new12!\",\"temporary\":true}"))
                    .andExpect(status().isNoContent());
            UserEntity changed = userRepository.findById(id).orElseThrow();
            assertThat(passwordEncoder.matches("Admin-new12!", changed.getPassword())).isTrue();
            assertThat(changed.isTemporaryPassword()).isTrue();

            changed.setTotpSecret("totp-secret");
            changed.setTotpEnabled(true);
            changed.setTotpLastUsedCounter(7L);
            changed.setLockedUntil(Instant.now().plusSeconds(60));
            changed.setPermanentlyLocked(true);
            changed.setMfaPermanentlyLocked(true);
            userRepository.saveAndFlush(changed);
            mockMvc.perform(delete("/api/admin/users/{id}/totp", id).with(admin()))
                    .andExpect(status().isNoContent());
            UserEntity resetTotp = userRepository.findById(id).orElseThrow();
            assertThat(resetTotp.isTotpEnabled()).isFalse();
            assertThat(resetTotp.getTotpSecret()).isNull();
            assertThat(resetTotp.getTotpLastUsedCounter()).isNull();
            mockMvc.perform(post("/api/admin/users/{id}/unlock", id).with(admin()))
                    .andExpect(status().isNoContent());
            UserEntity unlocked = userRepository.findById(id).orElseThrow();
            assertThat(unlocked.getLockedUntil()).isNull();
            assertThat(unlocked.isPermanentlyLocked()).isFalse();
            assertThat(unlocked.isMfaPermanentlyLocked()).isFalse();

            mockMvc.perform(
                            multipart("/api/admin/users/{id}/avatar", id)
                                    .file(
                                            new MockMultipartFile(
                                                    "file",
                                                    "avatar.png",
                                                    MediaType.IMAGE_PNG_VALUE,
                                                    ONE_PIXEL_PNG))
                                    .with(
                                            request -> {
                                                request.setMethod("PUT");
                                                return request;
                                            })
                                    .with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(
                            jsonPath("$.avatarUrl")
                                    .value(org.hamcrest.Matchers.startsWith("/avatars/")));
            assertThat(userAvatarRepository.existsById(id)).isTrue();
            mockMvc.perform(delete("/api/admin/users/{id}/avatar", id).with(admin()))
                    .andExpect(status().isNoContent());
            assertThat(userAvatarRepository.existsById(id)).isFalse();
        } finally {
            mockMvc.perform(delete("/api/admin/users/{id}", id).with(admin()))
                    .andExpect(status().isNoContent());
        }
    }

    @Test
    @Transactional
    void administratorCanRotateSigningKeyAndRevokeUserSessions() throws Exception {
        mockMvc.perform(get("/api/admin/keys").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        String rotatedKey =
                mockMvc.perform(post("/api/admin/keys/rotate").with(admin()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.active").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String rotatedKeyId = JSON.readTree(rotatedKey).get("id").asString();
        mockMvc.perform(get("/api/admin/keys?active=true").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + rotatedKeyId + "')]").isNotEmpty());

        UserSessionEntity session = session("user");
        UserSessionEntity secondSession = session("user");
        try {
            mockMvc.perform(
                            delete("/api/admin/sessions/{id}", session.getSessionId())
                                    .with(admin()))
                    .andExpect(status().isNoContent());
            assertThat(userSessionRepository.findBySessionId(session.getSessionId())).isEmpty();

            mockMvc.perform(delete("/api/admin/users/user/sessions").with(admin()))
                    .andExpect(status().isNoContent());
            assertThat(userSessionRepository.findBySessionId(secondSession.getSessionId()))
                    .isEmpty();
        } finally {
            // The test transaction rolls back key rotation and created sessions together.
        }
    }

    @Test
    void administratorCanRotateARegisteredClientSecret() throws Exception {
        RegisteredClient client = registeredClientRepository.findByClientId("demo-client");
        assertThat(client).isNotNull();
        try {
            String response =
                    mockMvc.perform(
                                    post("/api/admin/clients/{id}/secret", client.getId())
                                            .with(admin()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.clientSecret").isNotEmpty())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            String rotatedSecret = JSON.readTree(response).get("clientSecret").asString();
            RegisteredClient updated = registeredClientRepository.findById(client.getId());
            assertThat(updated).isNotNull();
            assertThat(passwordEncoder.matches(rotatedSecret, updated.getClientSecret())).isTrue();
        } finally {
            registeredClientRepository.save(client);
        }
    }

    @Test
    void administratorCanCreateMapperAndIdentityProviderThenRemoveThem() throws Exception {
        String id = "it-oidc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String providerBody =
                JSON.writeValueAsString(
                        new AdminIdentityProviderRequestDTO(
                                id,
                                "oidc",
                                "IT OIDC",
                                id,
                                "generic",
                                false,
                                false,
                                false,
                                "it-client",
                                "it-secret",
                                false,
                                false,
                                false,
                                false,
                                "sub",
                                false,
                                false,
                                99,
                                "always",
                                "import",
                                "https://idp.example.test/authorize",
                                "https://idp.example.test/token",
                                null,
                                null,
                                null,
                                "client_secret_basic",
                                "openid,profile",
                                "sub"));
        String providerResponse =
                mockMvc.perform(
                                post("/api/admin/identity-providers")
                                        .with(admin())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(providerBody))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String providerId = JSON.readTree(providerResponse).get("id").asString();
        try {
            mockMvc.perform(
                            put("/api/admin/identity-providers/{id}", providerId)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(providerBody.replace("IT OIDC", "Updated IT OIDC")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Updated IT OIDC"));

            String mapperResponse =
                    mockMvc.perform(
                                    post("/api/admin/identity-providers/{id}/mappers", providerId)
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"name\":\"it-email\",\"sourceClaim\":\"email\",\"target\":\"email\",\"mapperType\":\"user-attribute\",\"syncMode\":\"inherit\",\"addToIdToken\":true,\"addToAccessToken\":true}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.name").value("it-email"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            String mapperId = JSON.readTree(mapperResponse).get("id").asString();
            mockMvc.perform(
                            put(
                                            "/api/admin/identity-providers/{id}/mappers/{mapperId}",
                                            providerId,
                                            mapperId)
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"name\":\"it-email-updated\",\"sourceClaim\":\"email\",\"target\":\"email\",\"mapperType\":\"user-attribute\",\"syncMode\":\"inherit\",\"addToIdToken\":true,\"addToAccessToken\":true}"))
                    .andExpect(status().isOk());
            mockMvc.perform(
                            delete(
                                            "/api/admin/identity-providers/{id}/mappers/{mapperId}",
                                            providerId,
                                            mapperId)
                                    .with(admin()))
                    .andExpect(status().isNoContent());
        } finally {
            mockMvc.perform(delete("/api/admin/identity-providers/{id}", providerId).with(admin()))
                    .andExpect(status().isNoContent());
        }
        assertThat(JSON.readTree(providerResponse).get("registrationId").asString()).isEqualTo(id);
    }

    @Test
    void administratorCanCreateAndDeleteLocalizationOverride() throws Exception {
        String key = "it.test." + UUID.randomUUID().toString().replace("-", "");
        String body =
                "{\"locale\":\"tr\",\"bundle\":\"admin\",\"messageKey\":\""
                        + key
                        + "\",\"messageValue\":\"Test mesajı\"}";
        String response =
                mockMvc.perform(
                                post("/api/admin/settings/localization/messages")
                                        .with(admin())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.messageKey").value(key))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id = JSON.readTree(response).get("id").asLong();
        mockMvc.perform(get("/api/admin/settings/localization/messages").with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(
                        put("/api/admin/settings/localization/messages/{id}", id)
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body.replace("Test mesajı", "Güncel mesaj")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageValue").value("Güncel mesaj"));
        mockMvc.perform(delete("/api/admin/settings/localization/messages/{id}", id).with(admin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void administratorCanUpdateAndRestoreLocalizationSettings() throws Exception {
        String current =
                mockMvc.perform(get("/api/admin/settings/localization").with(admin()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var settings = JSON.readTree(current);
        boolean originalEnabled = settings.get("internationalizationEnabled").asBoolean();
        String defaultLocale = settings.get("defaultLocale").asString();
        String supportedLocales = settings.get("supportedLocales").toString();
        String original = localizationRequest(originalEnabled, defaultLocale, supportedLocales);
        try {
            mockMvc.perform(
                            put("/api/admin/settings/localization")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            localizationRequest(
                                                    !originalEnabled,
                                                    defaultLocale,
                                                    supportedLocales)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.internationalizationEnabled").value(!originalEnabled));
        } finally {
            mockMvc.perform(
                            put("/api/admin/settings/localization")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(original))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.internationalizationEnabled").value(originalEnabled));
        }
    }

    private UserSessionEntity session(String username) {
        long now = Instant.now().toEpochMilli();
        UserSessionEntity session = new UserSessionEntity();
        session.setPrimaryId(UUID.randomUUID().toString());
        session.setSessionId(UUID.randomUUID().toString());
        session.setPrincipalName(username);
        session.setCreationTime(now);
        session.setLastAccessTime(now);
        session.setMaxInactiveInterval(1800);
        session.setExpiryTime(now + 1_800_000);
        return userSessionRepository.saveAndFlush(session);
    }

    private static String localizationRequest(
            boolean internationalizationEnabled, String defaultLocale, String supportedLocales) {
        return "{\"internationalizationEnabled\":"
                + internationalizationEnabled
                + ",\"defaultLocale\":\""
                + defaultLocale
                + "\",\"supportedLocales\":"
                + supportedLocales
                + "}";
    }

    private static org.springframework.security.test.web.servlet.request
                    .SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor
            admin() {
        return jwt().jwt(token -> token.subject("admin"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
