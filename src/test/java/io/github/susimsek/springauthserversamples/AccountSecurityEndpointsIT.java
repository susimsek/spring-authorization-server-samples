package io.github.susimsek.springauthserversamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.susimsek.springauthserversamples.domain.LoginSettingsEntity;
import io.github.susimsek.springauthserversamples.domain.UserAction;
import io.github.susimsek.springauthserversamples.domain.UserActionTokenEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.LoginSettingsRepository;
import io.github.susimsek.springauthserversamples.repository.UserActionTokenRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AccountSecurityEndpointsIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserActionTokenRepository userActionTokenRepository;
    @Autowired private UserCredentialRepository userCredentialRepository;
    @Autowired private LoginSettingsRepository loginSettingsRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void accountMfaAndRecoveryStatusAreAvailableForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/account/mfa").with(account("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(get("/api/account/mfa/recovery-codes").with(account("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.warningThreshold").isNumber());
    }

    @Test
    void accountApiEnforcesAuthorizationAndReturnsLocalizedProblemDetails() throws Exception {
        mockMvc.perform(get("/api/account/mfa")).andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/api/account/mfa")
                                .with(
                                        jwt().authorities(
                                                        new SimpleGrantedAuthority(
                                                                "SCOPE_admin-api"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/account/password")
                                .with(account("user"))
                                .header("Accept-Language", "tr")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"wrong\",\"newPassword\":\"Valid-new12!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("invalid_current_password"))
                .andExpect(jsonPath("$.field").value("currentPassword"))
                .andExpect(jsonPath("$.title").value("API isteği başarısız"))
                .andExpect(jsonPath("$.detail").value("Mevcut parola geçersiz."));
    }

    @Test
    void mfaSetupAndRecoveryGenerationRequireEnabledOtp() throws Exception {
        mockMvc.perform(post("/api/account/mfa/setup").with(account("user")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/account/mfa/recovery-codes").with(account("user")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/account/mfa/enable")
                                .with(account("user"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"code\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mfaCanBeEnabledWithRecoveryCodesAndDisabledAgain() throws Exception {
        LoginSettingsEntity settings = loginSettingsRepository.findById(1L).orElseThrow();
        boolean previousOtpEnabled = settings.isOtpEnabled();
        boolean previousReusable = settings.isOtpCodeReusable();
        String username = "mfa-it-" + UUID.randomUUID();
        UserEntity user = new UserEntity(null, username, "{noop}Mfa-test12!", true, Set.of());
        user = userRepository.save(user);
        try {
            settings.setOtpEnabled(true);
            settings.setOtpCodeReusable(false);
            loginSettingsRepository.save(settings);

            String setup =
                    mockMvc.perform(post("/api/account/mfa/setup").with(account(username)))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.secret").isString())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            String secret = JsonSupport.read(setup);
            String enableCode = totp(secret, Instant.now().getEpochSecond() / 30);

            mockMvc.perform(
                            post("/api/account/mfa/enable")
                                    .with(account(username))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"code\":\"" + enableCode + "\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/account/mfa/recovery-codes").with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codes").isArray())
                    .andExpect(jsonPath("$.codes.length()").value(12));

            String disableCode = totp(secret, Instant.now().getEpochSecond() / 30 + 1);
            mockMvc.perform(
                            post("/api/account/mfa/disable")
                                    .with(account(username))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"code\":\"" + disableCode + "\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/account/mfa").with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        } finally {
            userRepository.deleteById(user.getId());
            settings.setOtpEnabled(previousOtpEnabled);
            settings.setOtpCodeReusable(previousReusable);
            loginSettingsRepository.save(settings);
        }
    }

    @Test
    void webAuthnEndpointsReturnEmptyPageAndRejectUnknownCredential() throws Exception {
        mockMvc.perform(get("/api/account/webauthn/credentials").with(account("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.totalElements").value(0));

        mockMvc.perform(
                        get("/api/account/webauthn/credentials/not-a-credential")
                                .with(account("user")))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        put("/api/account/webauthn/credentials/not-a-credential")
                                .with(account("user"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"label\":\"Laptop\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        delete("/api/account/webauthn/credentials/not-a-credential")
                                .with(account("user")))
                .andExpect(status().isNotFound());
    }

    @Test
    void registeredWebAuthnCredentialCanBeListedRenamedAndDeleted() throws Exception {
        String username = "webauthn-it-" + UUID.randomUUID();
        UserEntity user =
                userRepository.save(
                        new UserEntity(null, username, "{noop}Webauthn-test12!", true, Set.of()));
        Bytes credentialId = Bytes.random();
        userCredentialRepository.save(
                ImmutableCredentialRecord.builder()
                        .credentialId(credentialId)
                        .userEntityUserId(
                                io.github.susimsek.springauthserversamples.config.security
                                        .WebAuthnUserEntityRepository.userHandle(user.getId()))
                        .publicKey(new ImmutablePublicKeyCose(new byte[] {1, 2, 3}))
                        .credentialType(PublicKeyCredentialType.PUBLIC_KEY)
                        .signatureCount(0)
                        .uvInitialized(true)
                        .transports(Set.of(AuthenticatorTransport.INTERNAL))
                        .backupEligible(false)
                        .backupState(false)
                        .created(Instant.now())
                        .lastUsed(Instant.now())
                        .label("Original passkey")
                        .build());
        String credentialPath =
                "/api/account/webauthn/credentials/" + credentialId.toBase64UrlString();
        try {
            mockMvc.perform(get("/api/account/webauthn/credentials").with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(
                            jsonPath("$.content[0].credentialId")
                                    .value(credentialId.toBase64UrlString()))
                    .andExpect(jsonPath("$.content[0].label").value("Original passkey"));

            mockMvc.perform(get(credentialPath).with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.label").value("Original passkey"));

            mockMvc.perform(
                            put(credentialPath)
                                    .with(account(username))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"label\":\"Laptop passkey\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(credentialPath).with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.label").value("Laptop passkey"));

            mockMvc.perform(delete(credentialPath).with(account(username)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(credentialPath).with(account(username)))
                    .andExpect(status().isNotFound());
        } finally {
            userCredentialRepository.delete(credentialId);
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void passwordAndEmailVerificationContractsRejectInvalidRequests() throws Exception {
        mockMvc.perform(
                        put("/api/account/password")
                                .with(account("user"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"wrong\",\"newPassword\":\"Valid-new12!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("currentPassword"));

        mockMvc.perform(
                        put("/api/account/password")
                                .with(account("user"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"currentPassword\":\"user\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/account/send-verify-email").with(account("user")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accountPasswordCanBeChangedAndStoredEncoded() throws Exception {
        String username = "password-it-" + UUID.randomUUID();
        userRepository.save(
                new UserEntity(null, username, "{noop}Current-test12!", true, Set.of()));
        try {
            mockMvc.perform(
                            put("/api/account/password")
                                    .with(account(username))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"currentPassword\":\"Current-test12!\",\"newPassword\":\"Changed-test12!\"}"))
                    .andExpect(status().isNoContent());

            UserEntity changed = userRepository.findByUsername(username).orElseThrow();
            assertThat(passwordEncoder.matches("Changed-test12!", changed.getPassword())).isTrue();
        } finally {
            userRepository
                    .findByUsername(username)
                    .ifPresent(user -> userRepository.deleteById(user.getId()));
        }
    }

    @Test
    void emailVerificationTokenVerifiesTheAccountAndCannotBeReused() throws Exception {
        String username = "verify-email-it-" + UUID.randomUUID();
        UserEntity user = new UserEntity(null, username, "{noop}Verify-email12!", true, Set.of());
        user.setEmail(username + "@example.test");
        user.setEmailVerified(false);
        user = userRepository.save(user);
        UserActionTokenEntity token = new UserActionTokenEntity();
        token.setUser(user);
        token.setAction(UserAction.VERIFY_EMAIL);
        String rawToken = UUID.randomUUID().toString();
        token.setTokenHash(sha256(rawToken));
        token.setEmail(user.getEmail());
        token.setCredentialFingerprint(sha256(user.getPassword()));
        token.setIssuedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(60));
        token = userActionTokenRepository.save(token);
        try {
            mockMvc.perform(
                            post("/api/auth/verify-email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + rawToken + "\"}"))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findByUsername(username).orElseThrow().isEmailVerified())
                    .isTrue();
            assertThat(
                            userActionTokenRepository
                                    .findById(token.getId())
                                    .orElseThrow()
                                    .getConsumedAt())
                    .isNotNull();

            mockMvc.perform(
                            post("/api/auth/verify-email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"token\":\"" + rawToken + "\"}"))
                    .andExpect(status().isConflict());
        } finally {
            userActionTokenRepository.deleteById(token.getId());
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void requiredPasswordActionCanBeCompletedThroughTheAccountApi() throws Exception {
        String username = "required-action-it-" + UUID.randomUUID();
        UserEntity user = new UserEntity(null, username, "{noop}Temporary-pass12!", true, Set.of());
        user.setMustChangePassword(true);
        user.setTemporaryPassword(true);
        user = userRepository.save(user);
        try {
            mockMvc.perform(get("/api/required-actions").with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.key == 'UPDATE_PASSWORD')]").isNotEmpty());

            mockMvc.perform(
                            post("/api/required-actions/UPDATE_PASSWORD")
                                    .with(account(username))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"values\":{\"newPassword\":\"Completed-pass12!\"}}"))
                    .andExpect(status().isNoContent());

            UserEntity completed = userRepository.findByUsername(username).orElseThrow();
            assertThat(passwordEncoder.matches("Completed-pass12!", completed.getPassword()))
                    .isTrue();
            assertThat(completed.isMustChangePassword()).isFalse();
            assertThat(completed.isTemporaryPassword()).isFalse();
            mockMvc.perform(get("/api/required-actions").with(account(username)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[?(@.key == 'UPDATE_PASSWORD')]").isEmpty());
        } finally {
            userRepository
                    .findByUsername(username)
                    .ifPresent(saved -> userRepository.deleteById(saved.getId()));
        }
    }

    @Test
    void accountProfileAttributesCanBeUpdatedAndInvalidRequestIsRejected() throws Exception {
        String original =
                mockMvc.perform(get("/api/account/profile/attributes").with(account("user2")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String originalAttributes = JsonSupport.attributes(original);
        try {
            mockMvc.perform(
                            put("/api/account/profile/attributes")
                                    .with(account("user2"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"attributes\":{\"department\":[\"IT\"],\"employeeNumber\":[\"IT-001\"]}}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attributes.department[0]").value("IT"));

            mockMvc.perform(
                            put("/api/account/profile/attributes")
                                    .with(account("user2"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"attributes\":null}"))
                    .andExpect(status().isBadRequest());
        } finally {
            mockMvc.perform(
                            put("/api/account/profile/attributes")
                                    .with(account("user2"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"attributes\":" + originalAttributes + "}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void accountCanBeDeletedAfterPasswordConfirmation() throws Exception {
        String username = "delete-it-" + UUID.randomUUID();
        userRepository.save(new UserEntity(null, username, "{noop}Delete-test12!", true, Set.of()));
        mockMvc.perform(
                        delete("/api/account")
                                .with(account(username))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currentPassword\":\"Delete-test12!\"}"))
                .andExpect(status().isNoContent());
        assertThat(userRepository.findByUsername(username)).isEmpty();
    }

    private static JwtRequestPostProcessor account(String username) {
        return jwt().jwt(token -> token.subject(username))
                .authorities(new SimpleGrantedAuthority("SCOPE_account-api"));
    }

    private static String totp(String secret, long counter) throws GeneralSecurityException {
        byte[] key = base32(secret);
        byte[] hash;
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
        int offset = hash[hash.length - 1] & 0xf;
        int binary =
                ((hash[offset] & 0x7f) << 24)
                        | ((hash[offset + 1] & 0xff) << 16)
                        | ((hash[offset + 2] & 0xff) << 8)
                        | (hash[offset + 3] & 0xff);
        return "%06d".formatted(binary % 1_000_000);
    }

    private static byte[] base32(String value) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String normalized = value.replace("=", "").toUpperCase();
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char character : normalized.toCharArray()) {
            buffer = (buffer << 5) | alphabet.indexOf(character);
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                output.write((buffer >> bits) & 0xff);
            }
        }
        return output.toByteArray();
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            value.getBytes(
                                                    java.nio.charset.StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class JsonSupport {
        private static String read(String json) {
            int start = json.indexOf('"' + "secret" + '"');
            int colon = json.indexOf(':', start);
            int quote = json.indexOf('"', colon + 1);
            int end = json.indexOf('"', quote + 1);
            return json.substring(quote + 1, end);
        }

        private static String attributes(String json) {
            try {
                return tools.jackson.databind.json.JsonMapper.builder()
                        .build()
                        .readTree(json)
                        .get("attributes")
                        .toString();
            } catch (tools.jackson.core.JacksonException exception) {
                throw new IllegalArgumentException(
                        "Profile attributes could not be read", exception);
            }
        }
    }
}
