package io.github.susimsek.springauthserversamples.service.admin;

import io.github.susimsek.springauthserversamples.domain.UserAvatarEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AdminAvatarService {

    private static final long MAX_AVATAR_PIXELS = 4_000_000;
    private static final int MAX_AVATAR_DIMENSION = 4_096;

    private final AdminUserService adminUserService;
    private final UserAvatarRepository userAvatarRepository;
    private final AdminAuditEventService adminAuditEventService;

    @Transactional
    public AvatarView updateAvatar(Long id, MultipartFile file, String currentUsername) {
        UserEntity user = adminUserService.requireManageableUser(id, currentUsername);
        AvatarPayload payload = avatarPayload(file);
        UserAvatarEntity avatar =
                userAvatarRepository.findById(id).orElseGet(UserAvatarEntity::new);
        avatar.setUserId(id);
        if (avatar.getPublicId() == null) {
            avatar.setPublicId(UUID.randomUUID().toString());
        }
        avatar.setContentType(payload.contentType());
        avatar.setContent(payload.content());
        UserAvatarEntity saved = userAvatarRepository.saveAndFlush(avatar);
        adminAuditEventService.avatarUpdated(id);
        return new AvatarView(avatarUrl(saved.getPublicId(), saved.getUpdatedAt()));
    }

    @Transactional
    public void deleteAvatar(Long id, String currentUsername) {
        adminUserService.requireManageableUser(id, currentUsername);
        userAvatarRepository.deleteById(id);
        adminAuditEventService.avatarDeleted(id);
    }

    private static String avatarUrl(String publicId, Instant updatedAt) {
        return "/avatars/" + publicId + "?v=" + updatedAt.toEpochMilli();
    }

    private static AvatarPayload avatarPayload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AdminClientException.badRequest(
                    "avatar", "admin_avatar_empty", "Avatar file is required");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw AdminClientException.badRequest(
                    "avatar", "admin_avatar_too_large", "Avatar must not exceed 2 MiB");
        }
        try {
            byte[] content = file.getBytes();
            String contentType = imageContentType(content);
            if (contentType == null) {
                throw AdminClientException.badRequest(
                        "avatar",
                        "admin_avatar_invalid_type",
                        "Avatar must be a JPEG or PNG image");
            }
            return new AvatarPayload(content, contentType);
        } catch (IOException exception) {
            throw AdminClientException.badRequest(
                    "avatar", "admin_avatar_unreadable", "Avatar could not be read");
        }
    }

    private static String imageContentType(byte[] content) {
        try (ImageInputStream input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                return null;
            }
            java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_AVATAR_DIMENSION
                        || height > MAX_AVATAR_DIMENSION
                        || (long) width * height > MAX_AVATAR_PIXELS) {
                    throw AdminClientException.badRequest(
                            "avatar",
                            "admin_avatar_dimensions",
                            "Avatar dimensions must not exceed 4 megapixels");
                }
                return switch (reader.getFormatName().toLowerCase(java.util.Locale.ROOT)) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> null;
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            return null;
        }
    }

    private record AvatarPayload(byte[] content, String contentType) {}

    public record AvatarView(String avatarUrl) {}
}
