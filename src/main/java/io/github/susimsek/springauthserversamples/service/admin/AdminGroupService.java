package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRolesRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserDTO;
import io.github.susimsek.springauthserversamples.mapper.AdminGroupMapper;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.GroupRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
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
                Mappers.getMapper(AdminGroupMapper.class));
    }

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
                    "name", ApiErrorCode.GROUP_DUPLICATE_NAME, "Group name is already registered");
        }
        GroupEntity group =
                adminGroupMapper.toEntity(request, resolveParent(request.parentId(), null));
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
                    "name", ApiErrorCode.GROUP_DUPLICATE_NAME, "Group name is already registered");
        }
        adminGroupMapper.update(request, group);
        group.setParent(resolveParent(request.parentId(), group));
        invalidateUsersInGroupTree(group);
        adminAuditEventService.record("group.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional
    @CacheEvict(cacheNames = UserRepository.USER_BY_USERNAME_CACHE, allEntries = true)
    public AdminGroupDTO updateRoles(Long id, AdminGroupRolesRequestDTO request) {
        GroupEntity group = findGroup(id);
        adminGroupMapper.updateRoles(resolveAuthorities(request.roles()), group);
        invalidateUsersInGroupTree(group);
        adminAuditEventService.record("group.roles.updated", "group", group.getId().toString());
        return groupView(group);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> users(Long id, String query, Pageable pageable) {
        requireGroup(id);
        return userRepository
                .findByGroupsIdAndUsernameContainingIgnoreCase(
                        id, AdminSearch.normalize(query), pageable)
                .map(adminGroupMapper::toUserDTO);
    }

    @Transactional(readOnly = true)
    public Page<AdminGroupUserDTO> availableUsers(Long id, String query, Pageable pageable) {
        requireGroup(id);
        return userRepository
                .findAvailableGroupUsers(id, AdminSearch.normalize(query), pageable)
                .map(adminGroupMapper::toUserDTO);
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
        if (groupRepository.existsByParentId(id)) {
            throw ApiException.badRequest(
                    ApiErrorCode.GROUP_HAS_CHILDREN,
                    "Move or delete child groups before deleting this group");
        }
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

    private static String normalizeName(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw ApiException.badRequest(
                    "name", ApiErrorCode.GROUP_INVALID_NAME, "Group name is required");
        }
        return name;
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
