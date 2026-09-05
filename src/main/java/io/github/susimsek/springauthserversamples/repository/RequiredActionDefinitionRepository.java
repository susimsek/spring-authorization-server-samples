package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.RequiredActionDefinitionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequiredActionDefinitionRepository
        extends JpaRepository<RequiredActionDefinitionEntity, String> {

    List<RequiredActionDefinitionEntity> findAllByEnabledTrueOrderByPriorityAscActionKeyAsc();
}
