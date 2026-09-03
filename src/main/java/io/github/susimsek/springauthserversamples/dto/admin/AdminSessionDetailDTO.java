package io.github.susimsek.springauthserversamples.dto.admin;

import java.util.List;

public record AdminSessionDetailDTO(
        AdminSessionDTO session, List<AdminAuthorizationDTO> authorizations) {}
