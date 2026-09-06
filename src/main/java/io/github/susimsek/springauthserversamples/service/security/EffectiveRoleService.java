package io.github.susimsek.springauthserversamples.service.security;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves direct, group-inherited, and effective roles consistently. */
public final class EffectiveRoleService {

    private EffectiveRoleService() {}

    public static EffectiveRoles resolve(UserEntity user) {
        Set<String> assigned = roleNames(user.getAuthorities());
        List<GroupRoleMapping> mappings = new ArrayList<>();
        Set<String> groupRoles = new LinkedHashSet<>();
        if (user.getGroups() != null) {
            user.getGroups().stream()
                    .sorted(Comparator.comparing(EffectiveRoleService::path))
                    .forEach(
                            group -> {
                                Set<String> roles = rolesForGroup(group);
                                groupRoles.addAll(roles);
                                mappings.add(
                                        new GroupRoleMapping(group.getId(), path(group), roles));
                            });
        }
        Set<String> inherited = new LinkedHashSet<>(groupRoles);
        inherited.removeAll(assigned);
        Set<String> effective = new LinkedHashSet<>(assigned);
        effective.addAll(groupRoles);
        return new EffectiveRoles(
                immutableSorted(assigned),
                List.copyOf(mappings),
                immutableSorted(inherited),
                immutableSorted(effective));
    }

    public static Set<String> effectiveRoleNames(UserEntity user) {
        return resolve(user).effectiveRoles();
    }

    public static Set<String> effectiveGroupRoleNames(GroupEntity group) {
        return rolesForGroup(group);
    }

    private static Set<String> rolesForGroup(GroupEntity group) {
        Set<String> roles = new LinkedHashSet<>();
        Set<GroupEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        GroupEntity current = group;
        while (current != null && visited.add(current)) {
            roles.addAll(roleNames(current.getAuthorities()));
            current = current.getParent();
        }
        return immutableSorted(roles);
    }

    private static Set<String> roleNames(Set<AuthorityEntity> authorities) {
        if (authorities == null) {
            return Set.of();
        }
        return authorities.stream()
                .map(AuthorityEntity::getName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> immutableSorted(Set<String> values) {
        return values.stream().sorted().collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static String path(GroupEntity group) {
        List<String> names = new ArrayList<>();
        Set<GroupEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        GroupEntity current = group;
        while (current != null && visited.add(current)) {
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return "/" + String.join("/", names);
    }

    public record GroupRoleMapping(Long groupId, String groupPath, Set<String> roles) {}

    public record EffectiveRoles(
            Set<String> assignedRoles,
            List<GroupRoleMapping> groupMappings,
            Set<String> inheritedRoles,
            Set<String> effectiveRoles) {}
}
