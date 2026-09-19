package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.SocialProviderMapperEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.repository.SocialProviderMapperRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialIdentityMapperServiceTest {

    @Mock private SocialProviderMapperRepository mapperRepository;
    @Mock private UserProfileService userProfileService;

    @Test
    void appliesInheritedMappersOnFirstLogin() {
        SocialProviderMapperEntity mapper = mapper("email", "email", "inherit");
        when(mapperRepository.findAllByProviderAliasIgnoreCase("google"))
                .thenReturn(List.of(mapper));
        UserEntity user = new UserEntity();
        user.setEmail("old@example.test");

        service().apply("google", Map.of("email", "new@example.test"), user, true);

        assertThat(user.getEmail()).isEqualTo("new@example.test");
        verify(userProfileService).saveMappedUser(user, true);
    }

    @Test
    void doesNotApplyInheritedMapperToExistingUser() {
        SocialProviderMapperEntity mapper = mapper("email", "email", "inherit");
        when(mapperRepository.findAllByProviderAliasIgnoreCase("google"))
                .thenReturn(List.of(mapper));
        UserEntity user = new UserEntity();
        user.setEmail("old@example.test");

        service().apply("google", Map.of("email", "new@example.test"), user, false);

        assertThat(user.getEmail()).isEqualTo("old@example.test");
    }

    @Test
    void appliesForceMapperToExistingUserAndMergesCustomAttributes() {
        SocialProviderMapperEntity email = mapper("email", "email", "force");
        SocialProviderMapperEntity department = mapper("department", "department", "force");
        when(mapperRepository.findAllByProviderAliasIgnoreCase("google"))
                .thenReturn(List.of(email, department));
        UserEntity user = new UserEntity();
        user.setEmail("old@example.test");

        service()
                .apply(
                        "google",
                        Map.of("email", "new@example.test", "department", "Engineering"),
                        user,
                        false);

        assertThat(user.getEmail()).isEqualTo("new@example.test");
        verify(userProfileService)
                .mergeMappedAttributes(
                        user, Map.of("department", List.of("Engineering")), "social-login");
        verify(userProfileService).saveMappedUser(user, false);
    }

    @Test
    void collectsConfiguredTokenClaims() {
        SocialProviderMapperEntity mapper = mapper("department", "department", "force");
        mapper.setMapperType("claim");
        mapper.setAddToIdToken(true);
        when(mapperRepository.findAllByProviderAliasIgnoreCase("google"))
                .thenReturn(List.of(mapper));

        Map<String, Map<String, Object>> mapped =
                service()
                        .apply(
                                "google",
                                Map.of("department", "Engineering"),
                                new UserEntity(),
                                true);

        assertThat(mapped).containsEntry("id_token", Map.of("department", "Engineering"));
    }

    private SocialIdentityMapperService service() {
        return new SocialIdentityMapperService(mapperRepository, userProfileService);
    }

    private static SocialProviderMapperEntity mapper(
            String sourceClaim, String target, String syncMode) {
        SocialProviderMapperEntity mapper = new SocialProviderMapperEntity();
        mapper.setProviderAlias("google");
        mapper.setSourceClaim(sourceClaim);
        mapper.setTarget(target);
        mapper.setMapperType("user-attribute");
        mapper.setSyncMode(syncMode);
        return mapper;
    }
}
