package io.github.susimsek.springauthserversamples.service.admin;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.github.susimsek.springauthserversamples.domain.OAuth2KeyEntity;
import io.github.susimsek.springauthserversamples.repository.OAuth2KeyRepository;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeyManagementService {

    private final OAuth2KeyRepository oauth2KeyRepository;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<KeyView> keys(String query, Boolean active, Pageable pageable) {
        String searchQuery = AdminSearch.normalize(query);
        Page<OAuth2KeyEntity> keys =
                active == null
                        ? oauth2KeyRepository.findByKidContainingIgnoreCase(searchQuery, pageable)
                        : oauth2KeyRepository.findByKidContainingIgnoreCaseAndActive(
                                searchQuery, active, pageable);
        return keys.map(KeyManagementService::keyView);
    }

    @Transactional
    @CacheEvict(cacheNames = OAuth2KeyRepository.OAUTH2_KEYS_CACHE, allEntries = true)
    public KeyView rotateKey() {
        try {
            oauth2KeyRepository.findAllForRotation().forEach(key -> key.setActive(false));
            String kid = UUID.randomUUID().toString();
            RSAKey key =
                    new RSAKeyGenerator(2048)
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .keyID(kid)
                            .generate();
            OAuth2KeyEntity entity = new OAuth2KeyEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setKid(kid);
            entity.setType("RSA");
            entity.setAlgorithm("RS256");
            entity.setUse("sig");
            entity.setActive(true);
            entity.setPublicKey(
                    Base64.getEncoder().encodeToString(key.toRSAPublicKey().getEncoded()));
            entity.setPrivateKey(
                    Base64.getEncoder().encodeToString(key.toRSAPrivateKey().getEncoded()));
            OAuth2KeyEntity saved = oauth2KeyRepository.save(entity);
            adminAuditEventService.record("key.rotated", "key", saved.getId());
            return keyView(saved);
        } catch (Exception ex) {
            throw AdminClientException.serverError(
                    "admin_key_rotation_failed", "Could not rotate the signing key", ex);
        }
    }

    private static KeyView keyView(OAuth2KeyEntity key) {
        return new KeyView(
                key.getId(),
                key.getKid(),
                key.getType(),
                key.getAlgorithm(),
                key.getUse(),
                key.isActive(),
                key.getCreatedAt());
    }

    public record KeyView(
            String id,
            String kid,
            String type,
            String algorithm,
            String use,
            boolean active,
            Instant createdAt) {}
}
