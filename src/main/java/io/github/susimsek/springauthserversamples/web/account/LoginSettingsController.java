package io.github.susimsek.springauthserversamples.web.account;

import io.github.susimsek.springauthserversamples.dto.account.LoginSettingsDTO;
import io.github.susimsek.springauthserversamples.service.LoginSettingsService;
import io.github.susimsek.springauthserversamples.web.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiController
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Login settings", description = "Public login-page feature switches.")
public class LoginSettingsController {

    private final LoginSettingsService loginSettingsService;

    @GetMapping("/login-settings")
    @Operation(summary = "Get public login settings")
    @ApiResponse(responseCode = "200", description = "Public login settings returned.")
    LoginSettingsDTO get() {
        return loginSettingsService.publicLoginSettings();
    }
}
