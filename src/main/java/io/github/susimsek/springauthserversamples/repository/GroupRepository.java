package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.GroupEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    @EntityGraph(attributePaths = {"authorities", "parent"})
    Page<GroupEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByName(String name);

    boolean existsByParentId(Long parentId);

    @EntityGraph(attributePaths = "authorities")
    List<GroupEntity> findByParentId(Long parentId);

    @EntityGraph(attributePaths = {"authorities", "parent"})
    Optional<GroupEntity> findById(Long id);

    @EntityGraph(attributePaths = {"authorities", "parent"})
    @Query(
            "select g from UserEntity u join u.groups g where u.id = :userId"
                    + " and (:query = '' or lower(g.name) like lower(concat('%', :query, '%')))")
    Page<GroupEntity> findByUserIdAndNameContainingIgnoreCase(
            @Param("userId") Long userId, @Param("query") String query, Pageable pageable);
}
