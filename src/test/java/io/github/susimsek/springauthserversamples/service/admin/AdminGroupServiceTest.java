package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRolesRequestDTO;
import io.github.susimsek.springauthserversamples.repository.AuthorityRepository;
import io.github.susimsek.springauthserversamples.repository.GroupRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private AuthorityRepository authorityRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserAccessInvalidationService userAccessInvalidationService;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void findAllLoadsMemberCountsInOneBatch() {
        GroupEntity finance = group(7L, "finance");
        GroupEntity support = group(8L, "support");
        when(groupRepository.findByNameContainingIgnoreCase("", Pageable.ofSize(20)))
                .thenReturn(new PageImpl<>(List.of(finance, support), Pageable.ofSize(20), 2));
        when(userRepository.countUsersByGroupIdIn(List.of(7L, 8L)))
                .thenReturn(List.of(groupUserCount(7L, 3L), groupUserCount(8L, 1L)));

        var result = service().findAll("", Pageable.ofSize(20));

        assertThat(result.getContent()).extracting("userCount").containsExactly(3L, 1L);
        verify(userRepository, never()).countByGroupsId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void updateRolesInvalidatesEveryGroupMember() {
        GroupEntity group = group(7L, "finance");
        UserEntity alice = user(3L, "alice");
        UserEntity bob = user(4L, "bob");
        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        when(authorityRepository.findByNameIn(Set.of("ROLE_USER_VIEWER")))
                .thenReturn(List.of(authority("ROLE_USER_VIEWER")));
        when(userRepository.findAllByGroupsId(7L)).thenReturn(List.of(alice, bob));
        when(userRepository.countByGroupsId(7L)).thenReturn(2L);

        service().updateRoles(7L, new AdminGroupRolesRequestDTO(Set.of("ROLE_USER_VIEWER")));

        verify(userAccessInvalidationService).invalidate("alice");
        verify(userAccessInvalidationService).invalidate("bob");
    }

    @Test
    void updateRolesInvalidatesMembersOfNestedGroups() {
        GroupEntity parent = group(7L, "finance");
        GroupEntity child = group(8L, "operations");
        child.setParent(parent);
        UserEntity parentMember = user(3L, "alice");
        UserEntity childMember = user(4L, "bob");
        when(groupRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(groupRepository.findByParentId(7L)).thenReturn(List.of(child));
        when(groupRepository.findByParentId(8L)).thenReturn(List.of());
        when(authorityRepository.findByNameIn(Set.of("ROLE_USER_VIEWER")))
                .thenReturn(List.of(authority("ROLE_USER_VIEWER")));
        when(userRepository.findAllByGroupsId(7L)).thenReturn(List.of(parentMember));
        when(userRepository.findAllByGroupsId(8L)).thenReturn(List.of(childMember));
        when(userRepository.countByGroupsId(7L)).thenReturn(1L);

        service().updateRoles(7L, new AdminGroupRolesRequestDTO(Set.of("ROLE_USER_VIEWER")));

        verify(userAccessInvalidationService).invalidate("alice");
        verify(userAccessInvalidationService).invalidate("bob");
    }

    @Test
    void updateRejectsMovingGroupIntoItsDescendant() {
        GroupEntity parent = group(7L, "finance");
        GroupEntity child = group(8L, "operations");
        child.setParent(parent);
        when(groupRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(groupRepository.findById(8L)).thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service().update(7L, new AdminGroupRequestDTO("finance", 8L)))
                .hasMessageContaining("descendant");
    }

    @Test
    void addUserInvalidatesTheAddedUsersAccess() {
        GroupEntity group = group(7L, "finance");
        UserEntity alice = user(3L, "alice");
        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        when(userRepository.findById(3L)).thenReturn(Optional.of(alice));
        when(userRepository.countByGroupsId(7L)).thenReturn(1L);

        service().addUser(7L, 3L);

        verify(userAccessInvalidationService).invalidate("alice");
    }

    @Test
    void removeUserInvalidatesTheRemovedUsersAccess() {
        GroupEntity group = group(7L, "finance");
        UserEntity alice = user(3L, "alice");
        alice.getGroups().add(group);
        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        when(userRepository.findById(3L)).thenReturn(Optional.of(alice));
        when(userRepository.countByGroupsId(7L)).thenReturn(0L);

        service().removeUser(7L, 3L);

        verify(userAccessInvalidationService).invalidate("alice");
    }

    @Test
    void deleteInvalidatesEveryFormerGroupMember() {
        GroupEntity group = group(7L, "finance");
        UserEntity alice = user(3L, "alice");
        UserEntity bob = user(4L, "bob");
        alice.getGroups().add(group);
        bob.getGroups().add(group);
        when(groupRepository.findById(7L)).thenReturn(Optional.of(group));
        when(userRepository.findAllByGroupsId(7L)).thenReturn(List.of(alice, bob));

        service().delete(7L);

        verify(userAccessInvalidationService).invalidate("alice");
        verify(userAccessInvalidationService).invalidate("bob");
        verify(groupRepository).delete(group);
    }

    private AdminGroupService service() {
        return new AdminGroupService(
                groupRepository,
                authorityRepository,
                userRepository,
                userAccessInvalidationService,
                adminAuditEventService);
    }

    private static GroupEntity group(Long id, String name) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setName(name);
        return group;
    }

    private static UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private static AuthorityEntity authority(String name) {
        AuthorityEntity authority = new AuthorityEntity();
        authority.setName(name);
        return authority;
    }

    private static UserRepository.GroupUserCount groupUserCount(Long groupId, long userCount) {
        return new UserRepository.GroupUserCount() {
            @Override
            public Long getGroupId() {
                return groupId;
            }

            @Override
            public long getUserCount() {
                return userCount;
            }
        };
    }
}
