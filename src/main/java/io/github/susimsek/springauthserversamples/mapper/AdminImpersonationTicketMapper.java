package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.ImpersonationTicketEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminImpersonationTicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "consumedAt", ignore = true)
    ImpersonationTicketEntity toEntity(
            String ticketHash,
            String actorUsername,
            UserEntity targetUser,
            Instant issuedAt,
            Instant expiresAt);
}
