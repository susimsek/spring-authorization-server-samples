package io.github.susimsek.springauthserversamples.mapper;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileDTO;
import io.github.susimsek.springauthserversamples.dto.account.AccountProfileRequestDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountProfileMapper {

    AccountProfileDTO toDTO(UserEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    void updateEntity(AccountProfileRequestDTO source, @MappingTarget UserEntity target);
}
