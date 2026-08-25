package io.github.susimsek.springauthserversamples.repository;

import io.github.susimsek.springauthserversamples.domain.UserEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    String USER_BY_USERNAME_CACHE = "usersByUsername";

    @EntityGraph(value = "User.withEffectiveAuthorities")
    @Cacheable(cacheNames = USER_BY_USERNAME_CACHE)
    Optional<UserEntity> findByUsername(String username);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByUsernameContainingIgnoreCaseAndEnabled(
            String username, boolean enabled, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    java.util.List<UserEntity> findAllByUsernameIn(java.util.Collection<String> usernames);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByAuthoritiesName(String authorityName, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByAuthoritiesNameAndUsernameContainingIgnoreCase(
            String authorityName, String username, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    @Query(
            "select u from UserEntity u where"
                    + " (:query = '' or lower(u.username) like lower(concat('%', :query, '%')))"
                    + " and not exists (select a.id from u.authorities a where a.name = :roleName)")
    Page<UserEntity> findAvailableRoleUsers(
            @Param("roleName") String roleName, @Param("query") String query, Pageable pageable);

    long countByAuthoritiesName(String authorityName);

    long countByAuthoritiesId(Long authorityId);

    long countByGroupsId(Long groupId);

    @Query(
            "select g.id as groupId, count(u.id) as userCount from UserEntity u join u.groups g"
                    + " where g.id in :groupIds group by g.id")
    List<GroupUserCount> countUsersByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);

    @EntityGraph(value = "User.withAuthorities")
    Page<UserEntity> findByGroupsIdAndUsernameContainingIgnoreCase(
            Long groupId, String username, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    @Query(
            "select u from UserEntity u where"
                    + " (:query = '' or lower(u.username) like lower(concat('%', :query, '%')))"
                    + " and not exists (select g.id from u.groups g where g.id = :groupId)")
    Page<UserEntity> findAvailableGroupUsers(
            @Param("groupId") Long groupId, @Param("query") String query, Pageable pageable);

    @EntityGraph(value = "User.withAuthorities")
    java.util.List<UserEntity> findAllByGroupsId(Long groupId);

    interface GroupUserCount {
        Long getGroupId();

        long getUserCount();
    }
}
