package io.github.susimsek.springauthserversamples.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserAvatarEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminAvatarServiceTest {

    @Mock private AdminUserService adminUserService;
    @Mock private UserAvatarRepository userAvatarRepository;
    @Mock private AdminAuditEventService adminAuditEventService;

    @Test
    void uploadingAvatarUsesDetectedImageTypeAndVersionedUrl() {
        UserEntity user = new UserEntity();
        user.setId(5L);
        when(adminUserService.requireManageableUser(5L, "admin")).thenReturn(user);
        when(userAvatarRepository.findById(5L)).thenReturn(java.util.Optional.empty());
        when(userAvatarRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            UserAvatarEntity avatar = invocation.getArgument(0);
                            avatar.setPublicId("avatar-public-id");
                            avatar.setUpdatedAt(Instant.parse("2026-08-20T12:00:00Z"));
                            return avatar;
                        });

        var view =
                service()
                        .updateAvatar(
                                5L,
                                new MockMultipartFile(
                                        "file",
                                        "avatar.png",
                                        "text/plain",
                                        Base64.getDecoder()
                                                .decode(
                                                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9WlBdkcAAAAASUVORK5CYII=")),
                                "admin");

        assertThat(view.avatarUrl()).isEqualTo("/avatars/avatar-public-id?v=1787227200000");
        verify(userAvatarRepository).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verify(adminAuditEventService).avatarUpdated(5L);
    }

    @Test
    void uploadingNonImageAvatarIsRejected() {
        UserEntity user = new UserEntity();
        user.setId(5L);
        when(adminUserService.requireManageableUser(5L, "admin")).thenReturn(user);

        assertThatThrownBy(
                        () ->
                                service()
                                        .updateAvatar(
                                                5L,
                                                new MockMultipartFile(
                                                        "file",
                                                        "avatar.txt",
                                                        "image/png",
                                                        "not an image".getBytes()),
                                                "admin"))
                .isInstanceOf(AdminClientException.class)
                .hasMessage("Avatar must be a JPEG or PNG image");
    }

    @Test
    void deletingAvatarCreatesAdminAuditEvent() {
        service().deleteAvatar(5L, "admin");

        verify(adminUserService).requireManageableUser(5L, "admin");
        verify(userAvatarRepository).deleteById(5L);
        verify(adminAuditEventService).avatarDeleted(5L);
    }

    private AdminAvatarService service() {
        return new AdminAvatarService(
                adminUserService, userAvatarRepository, adminAuditEventService);
    }
}
