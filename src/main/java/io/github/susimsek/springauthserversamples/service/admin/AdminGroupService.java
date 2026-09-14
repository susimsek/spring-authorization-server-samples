package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.GroupPermissionEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupPermissionDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupPermissionsRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRolesRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserDTO;
import io.github.susimsek.springauthserversamples.mapper.AdminGroupMapper;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.GroupPermissionRepository;
import io.github.susimsek.springauthserversamples.repository.GroupRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import io.github.susimsek.springauthserversamples.service.security.EffectiveRoleService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administration operations for Keycloak-style groups and their realm-role mappings. */
@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final AdminAuditEventService adminAuditEventService;
    private final AdminGroupMapper adminGroupMapper;
    private final GroupPermissionRepository groupPermissionRepository;

    public AdminGroupService(
            GroupRepository groupRepository,
            AuthorityRepository authorityRepository,
            UserRepository userRepository,
            UserAccessInvalidationService userAccessInvalidationService,
            AdminAuditEventService adminAuditEventService) {
        this(
                groupRepository,
                authorityRepository,
                userRepository,
                userAccessInvalidationService,
                adminAuditEventService,
                Mappers.getMapper(AdminGroupMapper.class),
                null);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupDTO> findAll(String query, Pageable pageable) {
        return groupViews(
                groupRepository.findByNameContainingIgnoreCase(
                        AdminSearch.normalize(query), pageable),
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupDTO> findAll(String query, Pageable pageable, String currentUsername) {
        if (currentUsername == null || groupPermissionRepository == null) {
            return findAll(query, pageable);
        }
        UserEntity user = findUserByUsername(currentUsername);
        Set<String> roles = EffectiveRoleService.effectiveRoleNames(user);
        if (roles.contains(
                        io.github.susimsek.springauthserversamples.security.AuthoritiesConstants
                                .ADMIN)
                || roles.contains(
                        io.github.susimsek.springauthserversamples.security.AuthoritiesConstants
                                .USER_MANAGER)) {
            return findAll(query, pageable);
        }

        Set<Long> permissionRoots =
                groupPermissionRepository.findGroupIdsByUserIdAndPermissions(
                        user.getId(), GroupPermission.ALL);
        List<GroupEntity> visibleGroups =
                groupRepository.findAll().stream()
                        .filter(group -> hasPermissionRoot(group, permissionRoots))
                        .filter(
                                group ->
                                        AdminSearch.normalize(query).isEmpty()
                                                || group.getName()
                                                        .toLowerCase(java.util.Locale.ROOT)
                                                        .contains(
                                                                AdminSearch.normalize(query)
                                                                        .toLowerCase(
                                                                                java.util.Locale
                                                                                        .ROOT)))
                        .sorted(Comparator.comparing(GroupEntity::getName))
                        .toList();
        int from = (int) Math.min(pageable.getOffset(), visibleGroups.size());
        int to = Math.min(from + pageable.getPageSize(), visibleGroups.size());
        Page<GroupEntity> page =
                new PageImpl<>(visibleGroups.subList(from, to), pageable, visibleGroups.size());
        return groupViews(page, pageable);
    }

    @Transactional(readOnly = true)
    public AdminGroupDTO findById(Long id) {
        return findById(id, null);
    }

    @Transactional(readOnly = true)
    public AdminGroupDTO findById(Long id, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.VIEW);
        return groupView(group);
    }

    @Transactional
    public AdminGroupDTO create(AdminGroupRequestDTO request) {
        String name = normalizeName(request.name());
        if (groupRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", ApiErrorCode.GROUP_DUPLICATE_NAME, "Group name is already registered");
        }
        GroupEntity group =
                adminGroupMapper.toEntity(request, resolveParent(request.parentId(), null));
        adminGroupMapper.updateAttributes(normalizeAttributes(request.attributes()), group);
        group.setDefaultGroup(request.defaultGroupValue());
        AdminGroupDTO view = groupView(groupRepository.save(group));
        adminAuditEventService.record("group.created", "group", view.id().toString());
        return view;
    }

    @Transactional
    public AdminGroupDTO update(Long id, AdminGroupRequestDTO request) {
        return update(id, request, null);
    }

    @Transactional
    public AdminGroupDTO update(Long id, AdminGroupRequestDTO request, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_GROUP);
        String name = normalizeName(request.name());
        if (!group.getName().equals(name) && groupRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", ApiErrorCode.GROUP_DUPLICATE_NAME, "Group name is already registered");
        }
        adminGroupMapper.update(request, group);
        group.setParent(resolveParent(request.parentId(), group));
        adminGroupMapper.updateAttributes(normalizeAttributes(request.attributes()), group);
        group.setDefaultGroup(request.defaultGroupValue());
        invalidateUsersInGroupTree(group);
        adminAuditEventService.record("group.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO updateRoles(Long id, AdminGroupRolesRequestDTO request) {
        return updateRoles(id, request, null);
    }

    @Transactional
    public AdminGroupDTO updateRoles(
            Long id, AdminGroupRolesRequestDTO request, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_ROLES);
        adminGroupMapper.updateRoles(resolveAuthorities(request.roles()), group);
        invalidateUsersInGroupTree(group);
        adminAuditEventService.record("group.roles.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> users(Long id, String query, Pageable pageable) {
        return users(id, query, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> users(
            Long id, String query, Pageable pageable, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.VIEW);
        return userRepository
                .findByGroupsIdAndUsernameContainingIgnoreCase(
                        id, AdminSearch.normalize(query), pageable)
                .map(adminGroupMapper::toUserDTO);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> availableUsers(Long id, String query, Pageable pageable) {
        return availableUsers(id, query, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> availableUsers(
            Long id, String query, Pageable pageable, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_MEMBERS);
        return userRepository
                .findAvailableGroupUsers(id, AdminSearch.normalize(query), pageable)
                .map(adminGroupMapper::toUserDTO);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO addUser(Long id, Long userId) {
        return addUser(id, userId, null);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO addUser(Long id, Long userId, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_MEMBERS);
        UserEntity user = findUser(userId);
        user.getGroups().add(group);
        userAccessInvalidationService.invalidate(user.getUsername());
        adminAuditEventService.record("group.user.added", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO removeUser(Long id, Long userId) {
        return removeUser(id, userId, null);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO removeUser(Long id, Long userId, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_MEMBERS);
        UserEntity user = findUser(userId);
        user.getGroups().remove(group);
        userAccessInvalidationService.invalidate(user.getUsername());
        adminAuditEventService.record("group.user.removed", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void delete(Long id) {
        delete(id, null);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void delete(Long id, String currentUsername) {
        GroupEntity group = findGroup(id);
        assertPermission(group, currentUsername, GroupPermission.MANAGE_GROUP);
        if (groupRepository.existsByParentId(id)) {
            throw ApiException.badRequest(
                    ApiErrorCode.GROUP_HAS_CHILDREN,
                    "Move or delete child groups before deleting this group");
        }
        java.util.List<UserEntity> users = userRepository.findAllByGroupsId(id);
        users.forEach(user -> user.getGroups().remove(group));
        invalidateUsers(users);
        if (groupPermissionRepository != null) {
            groupPermissionRepository.deleteByGroupId(id);
        }
        groupRepository.delete(group);
        adminAuditEventService.record("group.deleted", "group", id.toString());
    }

    private GroupEntity findGroup(Long id) {
        return groupRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("Group not found"));
    }

    private void requireGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw ApiException.notFound("Group not found");
        }
    }

    private UserEntity findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () ->
                                ApiException.forbidden(
                                        ApiErrorCode.FORBIDDEN, "Group permission denied"));
    }

    private Set<AuthorityEntity> resolveAuthorities(Set<String> roles) {
        Set<String> roleNames = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        var authorities = authorityRepository.findByNameIn(roleNames);
        if (authorities.size() != roleNames.size()) {
            throw ApiException.badRequest(
                    "roles", ApiErrorCode.GROUP_INVALID_ROLES, "One or more roles are invalid");
        }
        return new LinkedHashSet<>(authorities);
    }

    private AdminGroupDTO groupView(GroupEntity group) {
        return groupView(group, userRepository.countByGroupsId(group.getId()));
    }

    private AdminGroupDTO groupView(GroupEntity group, long userCount) {
        return adminGroupMapper.toDTO(group, userCount);
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

    private void invalidateUsersInGroupTree(GroupEntity group) {
        invalidateUsers(userRepository.findAllByGroupsId(group.getId()));
        groupRepository.findByParentId(group.getId()).forEach(this::invalidateUsersInGroupTree);
    }

    private void invalidateUsers(java.util.List<UserEntity> users) {
        users.forEach(user -> userAccessInvalidationService.invalidate(user.getUsername()));
    }

    @Transactional(readOnly = true)
    public List<AdminGroupPermissionDTO> permissions(Long id) {
        requireGroup(id);
        if (groupPermissionRepository == null) {
            return List.of();
        }
        return groupPermissionRepository
                .findByGroupIdOrderByUserUsernameAscPermissionAsc(id)
                .stream()
                .map(
                        permission ->
                                new AdminGroupPermissionDTO(
                                        permission.getUser().getId(),
                                        permission.getUser().getUsername(),
                                        permission.getPermission()))
                .toList();
    }

    @Transactional
    public List<AdminGroupPermissionDTO> updatePermissions(
            Long id, AdminGroupPermissionsRequestDTO request) {
        GroupEntity group = findGroup(id);
        if (groupPermissionRepository == null) {
            return List.of();
        }
        Set<String> affectedUsernames =
                new java.util.LinkedHashSet<>(
                        groupPermissionRepository
                                .findByGroupIdOrderByUserUsernameAscPermissionAsc(id)
                                .stream()
                                .map(permission -> permission.getUser().getUsername())
                                .toList());
        List<GroupPermissionEntity> assignments = new ArrayList<>();
        for (var item : request.permissions()) {
            String permission = item.permission().strip().toUpperCase(java.util.Locale.ROOT);
            if (!GroupPermission.ALL.contains(permission)) {
                throw ApiException.badRequest(
                        "permissions",
                        ApiErrorCode.GROUP_INVALID_PERMISSION,
                        "Unknown group permission");
            }
            UserEntity user = findUser(item.userId());
            affectedUsernames.add(user.getUsername());
            assignments.add(new GroupPermissionEntity(group, user, permission));
        }
        groupPermissionRepository.deleteByGroupId(id);
        groupPermissionRepository.saveAll(assignments);
        affectedUsernames.forEach(userAccessInvalidationService::invalidate);
        adminAuditEventService.record("group.permissions.updated", "group", id.toString());
        return permissions(id);
    }

    private static String normalizeName(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw ApiException.badRequest(
                    "name", ApiErrorCode.GROUP_INVALID_NAME, "Group name is required");
        }
        return name;
    }

    private static Map<String, List<String>> normalizeAttributes(
            Map<String, List<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> normalized = new java.util.LinkedHashMap<>();
        attributes.forEach(
                (name, values) -> {
                    String normalizedName = name == null ? "" : name.strip();
                    if (normalizedName.isEmpty()
                            || normalizedName.length() > 100
                            || values == null
                            || values.isEmpty()
                            || values.size() > 20
                            || values.stream()
                                    .anyMatch(
                                            value ->
                                                    value == null
                                                            || value.isBlank()
                                                            || value.length() > 1000)) {
                        throw ApiException.badRequest(
                                "attributes",
                                ApiErrorCode.GROUP_INVALID_ATTRIBUTES,
                                "Group attributes are invalid");
                    }
                    normalized.put(normalizedName, values.stream().map(String::strip).toList());
                });
        return normalized;
    }

    private void assertPermission(GroupEntity group, String currentUsername, String permission) {
        if (currentUsername == null || groupPermissionRepository == null) {
            return;
        }
        UserEntity user = findUserByUsername(currentUsername);
        Set<String> roles = EffectiveRoleService.effectiveRoleNames(user);
        if (roles.contains(
                        io.github.susimsek.springauthserversamples.security.AuthoritiesConstants
                                .ADMIN)
                || roles.contains(
                        io.github.susimsek.springauthserversamples.security.AuthoritiesConstants
                                .USER_MANAGER)) {
            return;
        }
        if (!groupPermissionRepository.existsForUserAndGroups(
                user.getId(), groupIds(group), permission)) {
            throw ApiException.forbidden(ApiErrorCode.FORBIDDEN, "Group permission denied");
        }
    }

    private static Set<Long> groupIds(GroupEntity group) {
        Set<Long> ids = new java.util.LinkedHashSet<>();
        GroupEntity current = group;
        while (current != null) {
            ids.add(current.getId());
            current = current.getParent();
        }
        return ids;
    }

    private static boolean hasPermissionRoot(GroupEntity group, Set<Long> permissionRoots) {
        Set<Long> visited = new LinkedHashSet<>();
        GroupEntity current = group;
        while (current != null && visited.add(current.getId())) {
            if (permissionRoots.contains(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private GroupEntity resolveParent(Long parentId, GroupEntity group) {
        if (parentId == null) {
            return null;
        }
        if (group != null && parentId.equals(group.getId())) {
            throw ApiException.badRequest(
                    "parentId",
                    ApiErrorCode.GROUP_INVALID_PARENT,
                    "A group cannot be its own parent");
        }
        GroupEntity parent = findGroup(parentId);
        if (group != null && isDescendant(parent, group.getId())) {
            throw ApiException.badRequest(
                    "parentId",
                    ApiErrorCode.GROUP_INVALID_PARENT,
                    "A group cannot be moved into its descendant");
        }
        return parent;
    }

    private static boolean isDescendant(GroupEntity group, Long ancestorId) {
        GroupEntity current = group;
        while (current != null) {
            if (ancestorId.equals(current.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
