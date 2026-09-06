package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupRequestDTO;
import io.github.susimsek.springauthserversamples.dto.admin.AdminGroupUserDTO;
import io.github.susimsek.springauthserversamples.service.security.EffectiveRoleService;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name", qualifiedByName = "trimName")
    @Mapping(target = "parent", source = "parent")
    @Mapping(target = "authorities", ignore = true)
    GroupEntity toEntity(AdminGroupRequestDTO request, GroupEntity parent);

    @Mapping(target = "name", source = "name", qualifiedByName = "trimName")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    void update(AdminGroupRequestDTO request, @MappingTarget GroupEntity target);

    default void updateRoles(Set<AuthorityEntity> resolvedAuthorities, GroupEntity target) {
        target.setAuthorities(resolvedAuthorities);
    }

    default AdminGroupDTO toDTO(GroupEntity group, @Context Long userCount) {
        return new AdminGroupDTO(
                group.getId(),
                group.getName(),
                path(group),
                group.getParent() == null ? null : group.getParent().getId(),
                group.getAuthorities().stream()
                        .map(AuthorityEntity::getName)
                        .sorted()
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                EffectiveRoleService.effectiveGroupRoleNames(group),
                userCount == null ? 0L : userCount);
    }

    AdminGroupUserDTO toUserDTO(UserEntity entity);

    @Named("trimName")
    default String trimName(String name) {
        return name == null ? null : name.strip();
    }

    default String path(GroupEntity group) {
        java.util.Deque<String> names = new java.util.ArrayDeque<>();
        GroupEntity current = group;
        while (current != null) {
            names.addFirst(current.getName());
            current = current.getParent();
        }
        return String.join(" / ", names);
    }
}
