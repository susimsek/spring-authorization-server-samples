package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserProfileAttributeDefinitionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileAttributeDefinitionRepository
        extends JpaRepository<UserProfileAttributeDefinitionEntity, Long> {

    List<UserProfileAttributeDefinitionEntity> findAllByOrderByDisplayOrderAscNameAsc();

    List<UserProfileAttributeDefinitionEntity> findAllByEnabledTrueOrderByDisplayOrderAscNameAsc();

    Page<UserProfileAttributeDefinitionEntity>
            findByNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                    String name, String displayName, Pageable pageable);

    Optional<UserProfileAttributeDefinitionEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
