package io.github.susimsek.springauthserversamples.service;

import io.github.susimsek.springauthserversamples.domain.SocialProviderMapperEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.SocialProviderMapperRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the configured identity-provider mappers during broker login. */
@Service
@RequiredArgsConstructor
public class SocialIdentityMapperService {

    private final SocialProviderMapperRepository mapperRepository;
    private final UserProfileService userProfileService;

    @Transactional
    public Map<String, Map<String, Object>> apply(
            String providerAlias, Map<String, Object> claims, UserEntity user, boolean firstLogin) {
        if (providerAlias == null || providerAlias.isBlank()) {
            return Map.of();
        }
        List<SocialProviderMapperEntity> mappers =
                mapperRepository.findAllByProviderAliasIgnoreCase(providerAlias);
        if (mappers.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> profileValues = new LinkedHashMap<>();
        Map<String, Map<String, Object>> mappedClaims = new LinkedHashMap<>();
        boolean userChanged = false;
        for (SocialProviderMapperEntity mapper : mappers) {
            if (!isApplicable(mapper, firstLogin)) {
                continue;
            }
            List<String> values = values(claims, mapper.getSourceClaim());
            if (values.isEmpty()) {
                continue;
            }
            String target = mapper.getTarget() == null ? "" : mapper.getTarget().trim();
            if (target.isEmpty()) {
                continue;
            }
            if ("user-attribute".equalsIgnoreCase(mapper.getMapperType())) {
                if (applyBuiltIn(user, target, values.getFirst())) {
                    userChanged = true;
                } else if (!isBuiltInTarget(target)) {
                    profileValues.put(target, values);
                }
            }
            if (mapper.isAddToIdToken() || mapper.isAddToAccessToken()) {
                Object claimValue = values.size() == 1 ? values.getFirst() : values;
                if (!isReservedClaim(target)) {
                    if (mapper.isAddToIdToken()) {
                        mappedClaims
                                .computeIfAbsent("id_token", ignored -> new LinkedHashMap<>())
                                .put(target, claimValue);
                    }
                    if (mapper.isAddToAccessToken()) {
                        mappedClaims
                                .computeIfAbsent("access_token", ignored -> new LinkedHashMap<>())
                                .put(target, claimValue);
                    }
                }
            }
        }
        if (userChanged) {
            userProfileService.saveMappedUser(user, firstLogin);
        }
        if (!profileValues.isEmpty()) {
            userProfileService.mergeMappedAttributes(user, profileValues, "social-login");
        }
        return mappedClaims;
    }

    private static boolean isApplicable(SocialProviderMapperEntity mapper, boolean firstLogin) {
        String mode = mapper.getSyncMode() == null ? "inherit" : mapper.getSyncMode().trim();
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "force" -> true;
            // The sample has no realm-level provider override; inherit resolves to import.
            case "import", "legacy", "inherit" -> firstLogin;
            default -> firstLogin;
        };
    }

    private static boolean isBuiltInTarget(String target) {
        return switch (target) {
            case "username", "email", "firstName", "lastName", "pictureUrl", "emailVerified" ->
                    true;
            default -> false;
        };
    }

    private static boolean isReservedClaim(String target) {
        return switch (target) {
            case "iss", "sub", "aud", "exp", "iat", "nbf", "jti", "nonce", "sid", "scope" -> true;
            default -> false;
        };
    }

    private static boolean applyBuiltIn(UserEntity user, String target, String value) {
        if ("username".equals(target)) {
            // The application deliberately keeps provider/subject-derived usernames stable.
            return false;
        }
        return switch (target) {
            case "email" -> set(user.getEmail(), normalizeEmail(value), user::setEmail);
            case "firstName" -> set(user.getFirstName(), value, user::setFirstName);
            case "lastName" -> set(user.getLastName(), value, user::setLastName);
            case "pictureUrl" -> {
                String picture = httpsUrl(value);
                yield picture != null && set(user.getPictureUrl(), picture, user::setPictureUrl);
            }
            case "emailVerified" -> {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    yield false;
                }
                yield set(
                        user.isEmailVerified(),
                        Boolean.parseBoolean(value),
                        user::setEmailVerified);
            }
            default -> false;
        };
    }

    private static <T> boolean set(T current, T next, java.util.function.Consumer<T> setter) {
        if (next == null || Objects.equals(current, next)) {
            return false;
        }
        setter.accept(next);
        return true;
    }

    private static List<String> values(Map<String, Object> claims, String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        Object value = claims;
        for (String part : path.split("\\.")) {
            if (!(value instanceof Map<?, ?> map)) {
                return List.of();
            }
            value = map.get(part);
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> result = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item).trim());
                }
            }
            return result;
        }
        String scalar = value == null ? "" : String.valueOf(value).trim();
        return scalar.isBlank() ? List.of() : List.of(scalar);
    }

    private static String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String httpsUrl(String value) {
        if (value == null || value.length() > 1000) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                            && uri.getHost() != null
                            && uri.getUserInfo() == null
                            && uri.getFragment() == null
                    ? uri.toString()
                    : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
