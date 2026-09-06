package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleDetailDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminRoleUserDTO;
import io.github.susimsek.springauthserversamples.mapper.AdminRoleMapper;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.EffectiveRoleService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class AdminRoleService {

    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final AdminAuditEventService adminAuditEventService;
    private final AdminUserService adminUserService;
    private final AdminRoleMapper adminRoleMapper;

    public AdminRoleService(
            AuthorityRepository authorityRepository,
            UserRepository userRepository,
            AdminAuditEventService adminAuditEventService,
            AdminUserService adminUserService) {
        this(
                authorityRepository,
                userRepository,
                adminAuditEventService,
                adminUserService,
                Mappers.getMapper(AdminRoleMapper.class));
    }

    @Transactional(readOnly = true)
    public Page<AdminRoleDTO> roles(String query, Pageable pageable) {
        return authorityRepository
                .findByNameContainingIgnoreCase(AdminSearch.normalize(query), pageable)
                .map(adminRoleMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AdminRoleUserDTO> availableUsers(String name, String query, Pageable pageable) {
        if (!authorityRepository.existsByName(name)) {
            throw ApiException.notFound("Role not found");
        }
        String normalizedQuery = AdminSearch.normalize(query);
        java.util.List<UserEntity> allUsers = userRepository.findAllWithEffectiveAuthorities();
        if (allUsers != null && !allUsers.isEmpty()) {
            java.util.List<UserEntity> availableUsers =
                    allUsers.stream()
                            .filter(UserEntity::isEnabled)
                            .filter(
                                    user ->
                                            !EffectiveRoleService.effectiveRoleNames(user)
                                                    .contains(name))
                            .filter(
                                    user ->
                                            normalizedQuery.isEmpty()
                                                    || user.getUsername()
                                                            .toLowerCase(java.util.Locale.ROOT)
                                                            .contains(
                                                                    normalizedQuery.toLowerCase(
                                                                            java.util.Locale.ROOT)))
                            .sorted(java.util.Comparator.comparing(UserEntity::getUsername))
                            .toList();
            int start = (int) Math.min(pageable.getOffset(), availableUsers.size());
            int end = Math.min(start + pageable.getPageSize(), availableUsers.size());
            return new PageImpl<>(
                    availableUsers.subList(start, end).stream()
                            .map(adminRoleMapper::toUserDTO)
                            .toList(),
                    pageable,
                    availableUsers.size());
        }
        return userRepository
                .findAvailableRoleUsers(name, normalizedQuery, pageable)
                .map(adminRoleMapper::toUserDTO);
    }

    @Transactional(readOnly = true)
    public AdminRoleDetailDTO role(String name, String query, Pageable pageable) {
        AuthorityEntity role =
                authorityRepository
                        .findByName(name)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        String normalizedQuery = AdminSearch.normalize(query);
        java.util.List<UserEntity> allUsers = userRepository.findAllWithEffectiveAuthorities();
        if (allUsers == null) {
            allUsers = java.util.List.of();
        }
        java.util.List<UserEntity> effectiveUsers =
                allUsers.stream()
                        .filter(
                                user ->
                                        EffectiveRoleService.effectiveRoleNames(user)
                                                .contains(name))
                        .filter(
                                user ->
                                        normalizedQuery.isEmpty()
                                                || user.getUsername()
                                                        .toLowerCase(java.util.Locale.ROOT)
                                                        .contains(
                                                                normalizedQuery.toLowerCase(
                                                                        java.util.Locale.ROOT)))
                        .sorted(java.util.Comparator.comparing(UserEntity::getUsername))
                        .toList();
        Page<AdminRoleUserDTO> users;
        if (effectiveUsers.isEmpty()) {
            users =
                    userRepository
                            .findByAuthoritiesNameAndUsernameContainingIgnoreCase(
                                    name, normalizedQuery, pageable)
                            .map(adminRoleMapper::toUserDTO);
        } else {
            int start = (int) Math.min(pageable.getOffset(), effectiveUsers.size());
            int end = Math.min(start + pageable.getPageSize(), effectiveUsers.size());
            users =
                    new PageImpl<>(
                            effectiveUsers.subList(start, end).stream()
                                    .map(adminRoleMapper::toUserDTO)
                                    .toList(),
                            pageable,
                            effectiveUsers.size());
        }
        long userCount =
                effectiveUsers.isEmpty()
                        ? userRepository.countByAuthoritiesId(role.getId())
                        : effectiveUsers.size();
        return adminRoleMapper.toDetailDTO(
                role.getName(),
                role.getDescription(),
                userCount,
                AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.USER.equals(name),
                users);
    }

    @Transactional(readOnly = true)
    public AdminRoleDetailDTO role(String name, Pageable pageable) {
        return role(name, "", pageable);
    }

    @Transactional
    public AdminRoleDetailDTO assignUser(
            String name, Long userId, String currentUsername, Pageable pageable) {
        adminUserService.assignRole(userId, name, currentUsername);
        adminAuditEventService.record("role.user.assigned", "role", name);
        return role(name, pageable);
    }

    @Transactional
    public AdminRoleDetailDTO removeUser(
            String name, Long userId, String currentUsername, Pageable pageable) {
        adminUserService.removeRole(userId, name, currentUsername);
        adminAuditEventService.record("role.user.removed", "role", name);
        return role(name, pageable);
    }

    @Transactional
    @CacheEvict(cacheNames = AuthorityRepository.AUTHORITY_BY_NAME_CACHE, allEntries = true)
    public AdminRoleDTO createRole(String name) {
        return createRole(name, null);
    }

    @Transactional
    @CacheEvict(cacheNames = AuthorityRepository.AUTHORITY_BY_NAME_CACHE, allEntries = true)
    public AdminRoleDTO createRole(String name, String description) {
        validateRoleName(name);
        if (authorityRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", ApiErrorCode.ROLE_DUPLICATE_NAME, "Role is already registered");
        }
        AdminRoleDTO view =
                adminRoleMapper.toDTO(
                        authorityRepository.save(
                                adminRoleMapper.toEntity(name, normalizeDescription(description))));
        adminAuditEventService.record("role.created", "role", name);
        return view;
    }

    @Transactional
    @CacheEvict(cacheNames = AuthorityRepository.AUTHORITY_BY_NAME_CACHE, allEntries = true)
    public AdminRoleDTO updateRole(String name, String description) {
        AuthorityEntity role =
                authorityRepository
                        .findByName(name)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        role.setDescription(normalizeDescription(description));
        adminAuditEventService.record("role.updated", "role", name);
        return adminRoleMapper.toDTO(role);
    }

    @Transactional
    @CacheEvict(cacheNames = AuthorityRepository.AUTHORITY_BY_NAME_CACHE, allEntries = true)
    public void deleteRole(String name) {
        AuthorityEntity role =
                authorityRepository
                        .findByName(name)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        if (AuthoritiesConstants.ADMIN.equals(name) || AuthoritiesConstants.USER.equals(name)) {
            throw ApiException.badRequest(ApiErrorCode.ROLE_PROTECTED, "Role cannot be removed");
        }
        if (userRepository.countByAuthoritiesId(role.getId()) > 0) {
            throw ApiException.badRequest(
                    ApiErrorCode.ROLE_ASSIGNED, "Role is assigned to one or more users");
        }
        authorityRepository.delete(role);
        adminAuditEventService.record("role.deleted", "role", name);
    }

    private static void validateRoleName(String name) {
        if (name == null || !name.matches("ROLE_[A-Z0-9_]+")) {
            throw ApiException.badRequest(
                    "name",
                    ApiErrorCode.ROLE_INVALID_NAME,
                    "Role names must use ROLE_ uppercase format");
        }
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String value = description.strip();
        return value.isEmpty() ? null : value;
    }
}
