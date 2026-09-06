package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminUserDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminUserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "email", source = "normalizedEmail")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "pendingEmail", ignore = true)
    @Mapping(target = "authorities", source = "resolvedAuthorities")
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "mustChangePassword", ignore = true)
    @Mapping(target = "temporaryPassword", ignore = true)
    @Mapping(target = "failedLoginCount", ignore = true)
    @Mapping(target = "lastFailedLoginAt", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "temporaryLockoutCount", ignore = true)
    @Mapping(target = "permanentlyLocked", ignore = true)
    @Mapping(target = "mfaFailedAttemptCount", ignore = true)
    @Mapping(target = "mfaPermanentlyLocked", ignore = true)
    @Mapping(target = "totpSecret", ignore = true)
    @Mapping(target = "totpEnabled", ignore = true)
    @Mapping(target = "totpLastUsedCounter", ignore = true)
    @Mapping(
            target = "emailVerified",
            expression = "java(normalizedEmail != null && emailVerified)")
    UserEntity toEntity(
            String username,
            String normalizedEmail,
            boolean emailVerified,
            boolean enabled,
            String encodedPassword,
            Set<AuthorityEntity> resolvedAuthorities);

    default AdminUserDTO toDTO(UserEntity entity, @Context String avatarUrl) {
        return new AdminUserDTO(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.isEmailVerified(),
                entity.isEnabled(),
                entity.isPermanentlyLocked()
                        || entity.isMfaPermanentlyLocked()
                        || (entity.getLockedUntil() != null
                                && entity.getLockedUntil().isAfter(java.time.Instant.now())),
                entity.getLockedUntil(),
                entity.getFailedLoginCount(),
                entity.isMustChangePassword(),
                entity.isTemporaryPassword(),
                entity.isTotpEnabled(),
                avatarUrl,
                authorities(entity),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "email", source = "normalizedEmail")
    @Mapping(target = "password", source = "encodedPassword")
    @Mapping(target = "pendingEmail", ignore = true)
    @Mapping(target = "authorities", source = "resolvedAuthorities")
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "passwordChangedAt", ignore = true)
    @Mapping(target = "mustChangePassword", ignore = true)
    @Mapping(target = "temporaryPassword", ignore = true)
    @Mapping(target = "failedLoginCount", ignore = true)
    @Mapping(target = "lastFailedLoginAt", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "temporaryLockoutCount", ignore = true)
    @Mapping(target = "permanentlyLocked", ignore = true)
    @Mapping(target = "mfaFailedAttemptCount", ignore = true)
    @Mapping(target = "mfaPermanentlyLocked", ignore = true)
    @Mapping(target = "totpSecret", ignore = true)
    @Mapping(target = "totpEnabled", ignore = true)
    @Mapping(target = "totpLastUsedCounter", ignore = true)
    @Mapping(
            target = "emailVerified",
            expression = "java(normalizedEmail != null && emailVerified)")
    void update(
            String username,
            String normalizedEmail,
            boolean emailVerified,
            boolean enabled,
            String encodedPassword,
            Set<AuthorityEntity> resolvedAuthorities,
            @org.mapstruct.MappingTarget UserEntity target);

    default void updateAuthorities(Set<AuthorityEntity> resolvedAuthorities, UserEntity target) {
        target.setAuthorities(resolvedAuthorities);
    }

    default void updateEnabled(boolean enabled, UserEntity target) {
        target.setEnabled(enabled);
    }

    default Set<String> authorities(UserEntity entity) {
        return entity.getAuthorities().stream()
                .map(AuthorityEntity::getName)
                .collect(Collectors.toUnmodifiableSet());
    }
}
