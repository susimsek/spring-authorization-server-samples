package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.GroupRepository;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.security.AuthoritiesConstants;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserAvatarRepository userAvatarRepository;
    private final AuthorityRepository authorityRepository;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public UserView user(Long id, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        return userView(user, avatarUrl(user.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public UserView createUser(
            String username,
            String password,
            boolean enabled,
            Set<String> roles,
            String currentUsername) {
        validateUser(username, password);
        assertRoleAssignmentAllowed(roles, currentUsername);
        if (userRepository.findByUsername(username).isPresent()) {
            throw ApiException.conflict(
                    "username", "admin_user_duplicate_username", "Username is already registered");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(enabled);
        user.setAuthorities(resolveAuthorities(roles));
        UserEntity saved = userRepository.save(user);
        adminAuditEventService.record("user.created", "user", saved.getId().toString());
        return userView(saved, null);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public UserView updateUser(
            Long id, String username, boolean enabled, Set<String> roles, String currentUsername) {
        validateUser(username, null);
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        if (!user.getUsername().equals(username)
                && userRepository.findByUsername(username).isPresent()) {
            throw ApiException.conflict(
                    "username", "admin_user_duplicate_username", "Username is already registered");
        }
        if (user.getUsername().equals(currentUsername) && !enabled) {
            throw ApiException.badRequest(
                    "admin_user_protected", "You cannot disable your own account");
        }
        assertRoleAssignmentAllowed(roles, currentUsername);
        assertNotLastAdmin(user, requestedRoleNames(roles));
        userAccessInvalidationService.invalidate(user.getUsername());
        user.setUsername(username);
        user.setEnabled(enabled);
        user.setAuthorities(resolveAuthorities(roles));
        adminAuditEventService.record("user.updated", "user", user.getId().toString());
        return userView(user, avatarUrl(user.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void changePassword(Long id, String password, String currentUsername) {
        if (password == null || password.length() < 8) {
            throw ApiException.badRequest(
                    "password",
                    "admin_user_invalid_password",
                    "Password must be at least 8 characters");
        }
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        user.setPassword(passwordEncoder.encode(password));
        userAccessInvalidationService.invalidate(user.getUsername());
        adminAuditEventService.record("user.password.updated", "user", user.getId().toString());
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void deleteUser(Long id, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        if (user.getUsername().equals(currentUsername)) {
            throw ApiException.badRequest(
                    "admin_user_protected", "You cannot delete your own account");
        }
        assertNotLastAdmin(user, Set.of());
        userAccessInvalidationService.invalidate(user.getUsername());
        userRepository.delete(user);
        adminAuditEventService.record("user.deleted", "user", id.toString());
    }

    @Transactional(readOnly = true)
    public Page<UserView> users(String query, Boolean enabled, Pageable pageable) {
        String searchQuery = AdminSearch.normalize(query);
        Page<UserEntity> users =
                enabled == null
                        ? userRepository.findByUsernameContainingIgnoreCase(searchQuery, pageable)
                        : userRepository.findByUsernameContainingIgnoreCaseAndEnabled(
                                searchQuery, enabled, pageable);
        return new PageImpl<>(userViews(users.getContent()), pageable, users.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupDTO> groups(
            Long userId, String query, Pageable pageable, String currentUsername) {
        UserEntity user = findUser(userId);
        assertCanManageUser(user, currentUsername);
        Page<GroupEntity> groups =
                groupRepository.findByUserIdAndNameContainingIgnoreCase(
                        userId, AdminSearch.normalize(query), pageable);
        return groupViews(groups, pageable);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public UserView assignRole(Long id, String roleName, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        AuthorityEntity role =
                authorityRepository
                        .findByName(roleName)
                        .orElseThrow(() -> ApiException.notFound("Role not found"));
        if (user.getAuthorities().add(role)) {
            userAccessInvalidationService.invalidate(user.getUsername());
            adminAuditEventService.record("user.role.assigned", "user", id.toString());
        }
        return userView(user, avatarUrl(user.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public UserView removeRole(Long id, String roleName, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        Set<String> remaining =
                user.getAuthorities().stream()
                        .map(AuthorityEntity::getName)
                        .filter(name -> !name.equals(roleName))
                        .collect(java.util.stream.Collectors.toSet());
        assertNotLastAdmin(user, remaining);
        if (user.getAuthorities().removeIf(authority -> authority.getName().equals(roleName))) {
            if (user.getAuthorities().isEmpty()) {
                user.setAuthorities(resolveAuthorities(Set.of(AuthoritiesConstants.USER)));
            }
            userAccessInvalidationService.invalidate(user.getUsername());
            adminAuditEventService.record("user.role.removed", "user", id.toString());
        }
        return userView(user, avatarUrl(user.getId()));
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void setUserEnabled(Long id, boolean enabled, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        if (user.getUsername().equals(currentUsername) && !enabled) {
            throw ApiException.badRequest(
                    "admin_user_protected", "You cannot disable your own account");
        }
        if (!enabled) {
            assertNotLastAdmin(user, Set.of());
            userAccessInvalidationService.invalidate(user.getUsername());
        }
        user.setEnabled(enabled);
        adminAuditEventService.record("user.enabled.updated", "user", user.getId().toString());
    }

    @Transactional(readOnly = true)
    public UserEntity requireManageableUser(Long id, String currentUsername) {
        UserEntity user = findUser(id);
        assertCanManageUser(user, currentUsername);
        return user;
    }

    @Transactional(readOnly = true)
    public void assertCanManageUsername(String username, String currentUsername) {
        userRepository
                .findByUsername(username)
                .ifPresentOrElse(
                        user -> assertCanManageUser(user, currentUsername),
                        () -> {
                            throw ApiException.notFound("User not found");
                        });
    }

    private UserEntity findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private Set<AuthorityEntity> resolveAuthorities(Set<String> roles) {
        Set<String> requestedRoles = requestedRoleNames(roles);
        List<AuthorityEntity> authorities = authorityRepository.findByNameIn(requestedRoles);
        if (authorities.size() != requestedRoles.size()) {
            throw ApiException.badRequest(
                    "roles", "admin_user_invalid_roles", "One or more roles are invalid");
        }
        return Set.copyOf(authorities);
    }

    private void assertRoleAssignmentAllowed(Set<String> roles, String currentUsername) {
        Set<String> administratorRoles =
                authorities(userRepository.findByUsername(currentUsername).orElseThrow());
        Set<String> requestedRoles = requestedRoleNames(roles);
        if (!administratorRoles.contains(AuthoritiesConstants.ADMIN)
                && !administratorRoles.containsAll(requestedRoles)) {
            throw ApiException.forbidden(
                    "admin_role_escalation", "You can only assign roles you already have");
        }
    }

    private void assertCanManageUser(UserEntity target, String currentUsername) {
        Set<String> administratorRoles =
                authorities(userRepository.findByUsername(currentUsername).orElseThrow());
        if (!administratorRoles.contains(AuthoritiesConstants.ADMIN)
                && authorities(target).contains(AuthoritiesConstants.ADMIN)) {
            throw ApiException.forbidden(
                    "admin_user_protected", "Only an administrator can manage an administrator");
        }
    }

    private void assertNotLastAdmin(UserEntity user, Set<String> replacementRoles) {
        if (authorities(user).contains(AuthoritiesConstants.ADMIN)
                && !replacementRoles.contains(AuthoritiesConstants.ADMIN)
                && userRepository.countByAuthoritiesName(AuthoritiesConstants.ADMIN) <= 1) {
            throw ApiException.badRequest(
                    "admin_last_admin_protected", "The last administrator must be retained");
        }
    }

    private List<UserView> userViews(List<UserEntity> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, UserAvatarRepository.AvatarVersion> avatars =
                userAvatarRepository
                        .findVersionsByUserIdIn(users.stream().map(UserEntity::getId).toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        UserAvatarRepository.AvatarVersion::getUserId,
                                        java.util.function.Function.identity()));
        return users.stream()
                .map(user -> userView(user, avatarUrl(avatars.get(user.getId()))))
                .toList();
    }

    private String avatarUrl(Long userId) {
        return userAvatarRepository.findVersionByUserId(userId).map(this::avatarUrl).orElse(null);
    }

    private String avatarUrl(UserAvatarRepository.AvatarVersion avatar) {
        return avatar == null
                ? null
                : "/avatars/" + avatar.getPublicId() + "?v=" + avatar.getUpdatedAt().toEpochMilli();
    }

    private static UserView userView(UserEntity user, String avatarUrl) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                avatarUrl,
                authorities(user),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private AdminGroupDTO groupView(GroupEntity group) {
        return groupView(group, userRepository.countByGroupsId(group.getId()));
    }

    private AdminGroupDTO groupView(GroupEntity group, long userCount) {
        Set<String> roles =
                group.getAuthorities().stream()
                        .map(AuthorityEntity::getName)
                        .sorted()
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AdminGroupDTO(
                group.getId(),
                group.getName(),
                groupPath(group),
                group.getParent() == null ? null : group.getParent().getId(),
                roles,
                userCount);
    }

    private Page<AdminGroupDTO> groupViews(Page<GroupEntity> groups, Pageable pageable) {
        List<GroupEntity> content = groups.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, groups.getTotalElements());
        }
        Map<Long, Long> userCounts =
                userRepository
                        .countUsersByGroupIdIn(content.stream().map(GroupEntity::getId).toList())
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        UserRepository.GroupUserCount::getGroupId,
                                        UserRepository.GroupUserCount::getUserCount));
        return groups.map(group -> groupView(group, userCounts.getOrDefault(group.getId(), 0L)));
    }

    private static Set<String> authorities(UserEntity user) {
        return user.getAuthorities().stream()
                .map(AuthorityEntity::getName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<String> requestedRoleNames(Set<String> roles) {
        return roles == null || roles.isEmpty() ? Set.of(AuthoritiesConstants.USER) : roles;
    }

    private static String groupPath(GroupEntity group) {
        java.util.Deque<String> names = new java.util.ArrayDeque<>();
        GroupEntity current = group;
        while (current != null) {
            names.addFirst(current.getName());
            current = current.getParent();
        }
        return String.join(" / ", names);
    }

    private static void validateUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw ApiException.badRequest(
                    "username", "admin_user_invalid_username", "Username is required");
        }
        if (password != null && password.length() < 8) {
            throw ApiException.badRequest(
                    "password",
                    "admin_user_invalid_password",
                    "Password must be at least 8 characters");
        }
    }

    public record UserView(
            Long id,
            String username,
            boolean enabled,
            String avatarUrl,
            Set<String> authorities,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {}
}
