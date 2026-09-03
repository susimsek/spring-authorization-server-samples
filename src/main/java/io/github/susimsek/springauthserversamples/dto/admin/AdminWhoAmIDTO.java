package io.github.susimsek.springauthserversamples.dto.admin;

import java.util.List;
import java.util.Map;

public record AdminWhoAmIDTO(
        String username, List<String> authorities, Map<String, Boolean> access) {}
