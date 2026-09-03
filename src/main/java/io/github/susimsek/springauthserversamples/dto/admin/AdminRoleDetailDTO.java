package io.github.susimsek.springauthserversamples.dto.admin;

import org.springframework.data.domain.Page;

public record AdminRoleDetailDTO(
        String name, long userCount, boolean protectedRole, Page<AdminRoleUserDTO> users) {}
