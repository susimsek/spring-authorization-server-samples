package io.github.susimsek.springauthserversamples.service.requiredaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springauthserversamples.domain.RequiredActionDefinitionEntity;
import io.github.susimsek.springauthserversamples.domain.UserEntity;
import io.github.susimsek.springauthserversamples.domain.UserRequiredActionEntity;
import io.github.susimsek.springauthserversamples.repository.RequiredActionCompletionRepository;
import io.github.susimsek.springauthserversamples.repository.RequiredActionDefinitionRepository;
import io.github.susimsek.springauthserversamples.repository.UserRepository;
import io.github.susimsek.springauthserversamples.repository.UserRequiredActionRepository;
import io.github.susimsek.springauthserversamples.service.admin.AdminAuditEventService;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequiredActionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RequiredActionDefinitionRepository definitionRepository;
    @Mock private UserRequiredActionRepository assignmentRepository;
    @Mock private RequiredActionCompletionRepository completionRepository;
    @Mock private AdminAuditEventService auditEventService;

    @Test
    void includesGlobalAndUserAssignedActions() {
        UserEntity user = user(7L, "alice");
        user.setFirstName(null);
        user.setLastName(null);
        user.setEmail(null);
        RequiredActionDefinitionEntity terms = definition("TERMS_AND_CONDITIONS", true, true, 20);
        RequiredActionDefinitionEntity profile = definition("UPDATE_PROFILE", false, false, 10);
        UserRequiredActionEntity assignment = new UserRequiredActionEntity();
        assignment.setActionKey(profile.getActionKey());
        assignment.setVersion(1);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(assignmentRepository.findAllByUserId(7L)).thenReturn(List.of(assignment));
        when(definitionRepository.findAllByEnabledTrueOrderByPriorityAscActionKeyAsc())
                .thenReturn(List.of(terms));
        when(definitionRepository.findById("UPDATE_PROFILE")).thenReturn(Optional.of(profile));
        when(completionRepository.findTopByUserIdAndActionKeyOrderByVersionDesc(anyLong(), any()))
                .thenReturn(Optional.empty());

        assertThat(service().pending("alice"))
                .extracting(action -> action.key())
                .containsExactly("UPDATE_PROFILE", "TERMS_AND_CONDITIONS");
    }

    @Test
    void completesConfirmedGlobalActionAndAuditsIt() {
        UserEntity user = user(7L, "alice");
        RequiredActionDefinitionEntity terms = definition("TERMS_AND_CONDITIONS", true, true, 20);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.findForActionById(7L)).thenReturn(Optional.of(user));
        when(definitionRepository.findById("TERMS_AND_CONDITIONS")).thenReturn(Optional.of(terms));
        when(definitionRepository.findAllByEnabledTrueOrderByPriorityAscActionKeyAsc())
                .thenReturn(List.of(terms));
        when(assignmentRepository.findAllByUserId(7L)).thenReturn(List.of());
        when(assignmentRepository.findByUserIdAndActionKey(7L, "TERMS_AND_CONDITIONS"))
                .thenReturn(Optional.empty());
        when(completionRepository.findTopByUserIdAndActionKeyOrderByVersionDesc(
                        7L, "TERMS_AND_CONDITIONS"))
                .thenReturn(Optional.empty());

        service()
                .complete(
                        "alice",
                        "TERMS_AND_CONDITIONS",
                        Map.of("accepted", true),
                        "127.0.0.1",
                        "test-agent");

        verify(completionRepository).save(any());
        verify(auditEventService).record("user.required-action.completed", "user", "7");
    }

    private RequiredActionService service() {
        return new RequiredActionService(
                userRepository,
                definitionRepository,
                assignmentRepository,
                completionRepository,
                List.of(
                        new StandardRequiredActionHandler(
                                Validation.buildDefaultValidatorFactory().getValidator())),
                auditEventService);
    }

    private static UserEntity user(Long id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail("alice@example.test");
        user.setFirstName("Alice");
        user.setLastName("Tester");
        return user;
    }

    private static RequiredActionDefinitionEntity definition(
            String key, boolean enabled, boolean global, int priority) {
        RequiredActionDefinitionEntity definition = new RequiredActionDefinitionEntity();
        definition.setActionKey(key);
        definition.setDisplayName(key);
        definition.setDescription(key);
        definition.setEnabled(enabled);
        definition.setGlobalPolicy(global);
        definition.setVersion(1);
        definition.setPriority(priority);
        return definition;
    }
}
