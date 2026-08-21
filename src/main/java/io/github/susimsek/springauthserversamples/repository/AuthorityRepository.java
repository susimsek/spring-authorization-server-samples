package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.AuthorityEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {

    List<AuthorityEntity> findByNameIn(Iterable<String> names);

    List<AuthorityEntity> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    Optional<AuthorityEntity> findByName(String name);
}
