package io.github.susimsek.springauthserversamples.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.domain.UserProfileAttributeDefinitionEntity;
import io.github.susimsek.springauthserversamples.domain.UserProfileAttributeType;
import io.github.susimsek.springauthserversamples.dto.userprofile.UserProfileAttributeDefinitionRequestDTO;
import io.github.susimsek.springauthserversamples.dto.userprofile.UserProfileAttributeOrderRequestDTO;
import io.github.susimsek.springauthserversamples.repository.UserProfileAttributeDefinitionRepository;
import io.github.susimsek.springauthserversamples.repository.UserProfileAttributeRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import io.github.susimsek.springauthserversamples.service.admin.UserAccessInvalidationService;
import io.github.susimsek.springauthserversamples.service.error.ApiErrorCode;
import io.github.susimsek.springauthserversamples.service.error.ApiException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileAttributeDefinitionRepository definitionRepository;
    @Mock private UserProfileAttributeRepository attributeRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminAuditEventService auditEventService;
    @Mock private UserAccessInvalidationService userAccessInvalidationService;
    @Mock private EntityManager entityManager;

    @Test
    void rejectsReservedAndInvalidDefinitionNames() {
        UserProfileService service = service();

        assertThatThrownBy(() -> service.create(request("email", UserProfileAttributeType.STRING)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.USER_PROFILE_INVALID_NAME);

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new UserProfileAttributeDefinitionRequestDTO(
                                                "department",
                                                "Department",
                                                null,
                                                UserProfileAttributeType.STRING,
                                                false,
                                                false,
                                                10,
                                                2,
                                                null,
                                                true,
                                                0)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.USER_PROFILE_INVALID_DEFINITION);
    }

    @Test
    void rejectsMissingRequiredAndInvalidTypedValues() {
        UserProfileAttributeDefinitionEntity definition = definition("age", true);
        definition.setType(UserProfileAttributeType.INTEGER);
        when(definitionRepository.findAllByEnabledTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(definition));

        UserEntity user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        UserProfileService service = service();

        assertThatThrownBy(() -> service.saveAttributes(7L, Map.of(), "admin"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.USER_PROFILE_REQUIRED);

        assertThatThrownBy(
                        () ->
                                service.saveAttributes(
                                        7L, Map.of("age", List.of("not-a-number")), "admin"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ApiErrorCode.USER_PROFILE_INVALID_VALUE);
    }

    @Test
    void savesNormalizedValuesAndInvalidatesAccess() {
        UserProfileAttributeDefinitionEntity definition = definition("department", false);
        when(definitionRepository.findAllByEnabledTrueOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(definition));
        when(definitionRepository.findAllByOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(definition));
        when(attributeRepository
                        .findAllByUserIdOrderByDefinitionDisplayOrderAscDefinitionNameAscPositionAsc(
                                7L))
                .thenReturn(List.of());
        UserEntity user = user();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        var result =
                service().saveAttributes(7L, Map.of("department", List.of("  Finance  ")), "admin");

        assertThat(result.definitions()).hasSize(1);
        verify(attributeRepository).deleteAllByUserId(7L);
        ArgumentCaptor<List> savedValues = ArgumentCaptor.forClass(List.class);
        verify(attributeRepository).saveAll(savedValues.capture());
        assertThat(savedValues.getValue()).hasSize(1);
        verify(userAccessInvalidationService).invalidate("alice");
        verify(auditEventService).record("user-profile.attributes.updated", "user", "7");
    }

    @Test
    void searchesPagedProfileDefinitions() {
        UserProfileAttributeDefinitionEntity definition = definition("department", false);
        var pageable = PageRequest.of(1, 10);
        when(definitionRepository.findByNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                        "dep", "dep", pageable))
                .thenReturn(
                        new org.springframework.data.domain.PageImpl<>(
                                List.of(definition), pageable, 11));

        var result = service().definitions("  dep  ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(11);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void reordersProfileDefinitionsAndAuditsTheChange() {
        UserProfileAttributeDefinitionEntity first = definition("first", false);
        first.setId(1L);
        first.setDisplayOrder(10);
        UserProfileAttributeDefinitionEntity second = definition("second", false);
        second.setId(2L);
        second.setDisplayOrder(20);
        when(definitionRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(first, second));
        when(definitionRepository.findAllByOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(second, first));

        var result = service().reorder(new UserProfileAttributeOrderRequestDTO(List.of(2L, 1L)));

        assertThat(second.getDisplayOrder()).isEqualTo(10);
        assertThat(first.getDisplayOrder()).isEqualTo(11);
        assertThat(result).extracting("name").containsExactly("second", "first");
        verify(definitionRepository).saveAll(List.of(first, second));
        verify(auditEventService)
                .record("user-profile.attributes.reordered", "user-profile-attributes", "order");
    }

    private UserProfileService service() {
        return new UserProfileService(
                definitionRepository,
                attributeRepository,
                userRepository,
                auditEventService,
                userAccessInvalidationService,
                entityManager);
    }

    private static UserProfileAttributeDefinitionRequestDTO request(
            String name, UserProfileAttributeType type) {
        return new UserProfileAttributeDefinitionRequestDTO(
                name, "Display", null, type, false, false, null, null, null, true, 0);
    }

    private static UserProfileAttributeDefinitionEntity definition(String name, boolean required) {
        UserProfileAttributeDefinitionEntity definition =
                new UserProfileAttributeDefinitionEntity();
        definition.setId(3L);
        definition.setName(name);
        definition.setDisplayName(name);
        definition.setType(UserProfileAttributeType.STRING);
        definition.setRequired(required);
        definition.setEnabled(true);
        return definition;
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("alice");
        return user;
    }
}
