package io.github.susimsek.springauthserversamples.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.dto.userprofile.UserProfileAttributeDefinitionDTO;
import io.github.susimsek.springauthserversamples.dto.userprofile.UserProfileAttributeOrderRequestDTO;
import io.github.susimsek.springauthserversamples.service.UserProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminUserProfileSettingsControllerTest {

    @Test
    void delegatesProfileAttributeReordering() {
        UserProfileService service = mock(UserProfileService.class);
        var controller = new AdminUserProfileSettingsController(service);
        var request = new UserProfileAttributeOrderRequestDTO(List.of(2L, 1L));
        var definitions = List.of(mock(UserProfileAttributeDefinitionDTO.class));
        when(service.reorder(request)).thenReturn(definitions);

        assertThat(controller.reorder(request)).isSameAs(definitions);
    }
}
