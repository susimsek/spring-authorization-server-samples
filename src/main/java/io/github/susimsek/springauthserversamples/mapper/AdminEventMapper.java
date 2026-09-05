package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.AdminEventEntity;
import io.github.susimsek.springauthserversamples.dto.admin.AdminEventDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminEventMapper {

    AdminEventDTO toDTO(AdminEventEntity entity);

    AdminEventEntity toEntity(
            String id,
            String actor,
            String action,
            String targetType,
            String targetId,
            java.time.Instant occurredAt);
}
