package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserProfileAttributeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileAttributeRepository
        extends JpaRepository<UserProfileAttributeEntity, Long> {

    List<UserProfileAttributeEntity>
            findAllByUserIdOrderByDefinitionDisplayOrderAscDefinitionNameAscPositionAsc(
                    Long userId);

    boolean existsByDefinitionId(Long definitionId);

    void deleteAllByUserId(Long userId);

    void deleteAllByUserIdAndDefinitionId(Long userId, Long definitionId);

    void deleteAllByDefinitionId(Long definitionId);
}
