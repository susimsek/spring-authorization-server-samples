package io.github.susimsek.springauthserversamples.web;

import io.github.susimsek.springauthserversamples.repository.UserAvatarRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AvatarController {

    private final UserAvatarRepository userAvatarRepository;
    private final UserRepository userRepository;

    @GetMapping("/avatars/{id}")
    ResponseEntity<byte[]> avatar(
            @PathVariable String id,
            @RequestParam("v") long version,
            @org.springframework.web.bind.annotation.RequestHeader(
                            value = HttpHeaders.IF_NONE_MATCH,
                            required = false)
                    String ifNoneMatch) {
        return userAvatarRepository
                .findByPublicId(id)
                .filter(avatar -> avatar.getUpdatedAt().toEpochMilli() == version)
                .map(
                        avatar ->
                                avatarResponse(
                                        id,
                                        ifNoneMatch,
                                        avatar,
                                        CacheControl.maxAge(Duration.ofDays(365))
                                                .cachePublic()
                                                .immutable()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping({"/account/avatar", "/api/account/avatar"})
    ResponseEntity<byte[]> currentUserAvatar(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestHeader(
                            value = HttpHeaders.IF_NONE_MATCH,
                            required = false)
                    String ifNoneMatch) {
        return userRepository
                .findByUsername(authentication.getName())
                .flatMap(user -> userAvatarRepository.findById(user.getId()))
                .map(avatar -> avatarResponse(avatar.getPublicId(), ifNoneMatch, avatar))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> avatarResponse(
            String id,
            String ifNoneMatch,
            io.github.susimsek.springauthserversamples.domain.UserAvatarEntity avatar) {
        CacheControl cacheControl = CacheControl.noCache().cachePrivate();
        return avatarResponse(id, ifNoneMatch, avatar, cacheControl);
    }

    private static ResponseEntity<byte[]> avatarResponse(
            String id,
            String ifNoneMatch,
            io.github.susimsek.springauthserversamples.domain.UserAvatarEntity avatar,
            CacheControl cacheControl) {
        String etag = "\"avatar-" + id + "-" + avatar.getUpdatedAt().toEpochMilli() + "\"";
        if (ifNoneMatch != null && ifNoneMatch.contains(etag)) {
            return ResponseEntity.status(304).eTag(etag).cacheControl(cacheControl).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .body(avatar.getContent());
    }
}
