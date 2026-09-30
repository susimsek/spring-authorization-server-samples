package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.UserEventEntity;
import io.github.susimsek.springauthserversamples.domain.UserEventType;
import io.github.susimsek.springauthserversamples.dto.admin.UserEventDTO;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserEventMapper {

    UserEventDTO toDTO(UserEventEntity entity);

    UserEventEntity toEntity(
            String id,
            Long userId,
            String username,
            UserEventType type,
            String clientId,
            String ipAddress,
            Instant occurredAt);
}
