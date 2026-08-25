package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRolesRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserDTO;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.GroupRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administration operations for Keycloak-style groups and their realm-role mappings. */
@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final AuthorityRepository authorityRepository;
    private final UserRepository userRepository;
    private final UserAccessInvalidationService userAccessInvalidationService;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional(readOnly = true)
    public Page<AdminGroupDTO> findAll(String query, Pageable pageable) {
        return groupViews(
                groupRepository.findByNameContainingIgnoreCase(
                        AdminSearch.normalize(query), pageable),
                pageable);
    }

    @Transactional(readOnly = true)
    public AdminGroupDTO findById(Long id) {
        return groupView(findGroup(id));
    }

    @Transactional
    public AdminGroupDTO create(AdminGroupRequestDTO request) {
        String name = normalizeName(request.name());
        if (groupRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", "group_duplicate_name", "Group name is already registered");
        }
        GroupEntity group = new GroupEntity();
        group.setName(name);
        AdminGroupDTO view = groupView(groupRepository.save(group));
        adminAuditEventService.record("group.created", "group", view.id().toString());
        return view;
    }

    @Transactional
    public AdminGroupDTO update(Long id, AdminGroupRequestDTO request) {
        GroupEntity group = findGroup(id);
        String name = normalizeName(request.name());
        if (!group.getName().equals(name) && groupRepository.existsByName(name)) {
            throw ApiException.conflict(
                    "name", "group_duplicate_name", "Group name is already registered");
        }
        group.setName(name);
        adminAuditEventService.record("group.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO updateRoles(Long id, AdminGroupRolesRequestDTO request) {
        GroupEntity group = findGroup(id);
        group.setAuthorities(resolveAuthorities(request.roles()));
        invalidateUsers(group);
        adminAuditEventService.record("group.roles.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> users(Long id, String query, Pageable pageable) {
        requireGroup(id);
        return userRepository
                .findByGroupsIdAndUsernameContainingIgnoreCase(
                        id, AdminSearch.normalize(query), pageable)
                .map(AdminGroupService::userView);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> availableUsers(Long id, String query, Pageable pageable) {
        requireGroup(id);
        return userRepository
                .findAvailableGroupUsers(id, AdminSearch.normalize(query), pageable)
                .map(AdminGroupService::userView);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO addUser(Long id, Long userId) {
        GroupEntity group = findGroup(id);
        UserEntity user = findUser(userId);
        user.getGroups().add(group);
        userAccessInvalidationService.invalidate(user.getUsername());
        adminAuditEventService.record("group.user.added", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO removeUser(Long id, Long userId) {
        GroupEntity group = findGroup(id);
        UserEntity user = findUser(userId);
        user.getGroups().remove(group);
        userAccessInvalidationService.invalidate(user.getUsername());
        adminAuditEventService.record("group.user.removed", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public void delete(Long id) {
        GroupEntity group = findGroup(id);
        java.util.List<UserEntity> users = userRepository.findAllByGroupsId(id);
        users.forEach(user -> user.getGroups().remove(group));
        invalidateUsers(users);
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

    private Set<AuthorityEntity> resolveAuthorities(Set<String> roles) {
        Set<String> roleNames = roles == null ? Set.of() : new LinkedHashSet<>(roles);
        var authorities = authorityRepository.findByNameIn(roleNames);
        if (authorities.size() != roleNames.size()) {
            throw ApiException.badRequest(
                    "roles", "group_invalid_roles", "One or more roles are invalid");
        }
        return new LinkedHashSet<>(authorities);
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
        return new AdminGroupDTO(group.getId(), group.getName(), roles, userCount);
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

    private static AdminGroupUserDTO userView(UserEntity user) {
        return new AdminGroupUserDTO(user.getId(), user.getUsername(), user.isEnabled());
    }

    private void invalidateUsers(GroupEntity group) {
        invalidateUsers(userRepository.findAllByGroupsId(group.getId()));
    }

    private void invalidateUsers(java.util.List<UserEntity> users) {
        users.forEach(user -> userAccessInvalidationService.invalidate(user.getUsername()));
    }

    private static String normalizeName(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw ApiException.badRequest("name", "group_invalid_name", "Group name is required");
        }
        return name;
    }
}
